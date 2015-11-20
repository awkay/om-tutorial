# Om Tutorial Code

This is a project holding an Om (next) project that is to be used
as the basis of a complete overview of the features of Om. For 
simplicity, the server-side component is simulated in the browser. 
The code you would write on the server is identical (though you 
will need some minor plumbing to do the actual network bits).

## What's inside?

```
├── cards
│   └── om_tutorial
│       ├── cards.cljs
│       └── parsing_cards.cljs     Devcards with parsing tests
├── project.clj
├── resources
│   └── public
│       ├── cards.html             HTML pages for accessing cards and UI
│       └── index.html
├── script
│   └── figwheel.clj               Dev run script
└── src
    └── om_tutorial
        ├── client_mutation.cljs   Functions that mutate the client state
        ├── client_remoting.cljs   Remote read (for parsing) and send functions (for sim server comms)
        ├── core.cljs              Entry point
        ├── local_read.cljs        Function to read local state (for parsing)
        ├── parsing.cljs           Helper functions to make parsing easy!
        ├── simulated_server.cljs  Simulated server in the browser (using setTimeout)
        └── ui.cljs                The UI components
```

## Running it

There is a clojure script in the `script` folder. Simply run that in Cursive (Run..., Add a Clojure Local REPL, Run with Clojure Main, Argument `script/figwheel.clj`)
or at the command line with:

```
lein run -m clojure.main script/figwheel.clj
```

The URLs are:

```
http://localhost:3450/             - Main app
http://localhost:3450/cards.html   - Devcards UI
```

## Parsing

### Local vs. Remote

The Om parser accepts just one read and one mutate. Unfortunately, this means that the same code gets invoked
for both the local query processing (to data for rendering) and again for asking "what do you want from remote(s)".

I've written a factory function that generates a dispatcher that separates this logic called
`new-read-entry-point` (see `om-tutorial.core` for an example use). The generated function also has the ability
to switch to dispatching to an alternate function during processing simply by associating that new function
with the `:reader` key in `env`.

This allows you to control which reader function is used based on the structure of the query instead of the keywords; 
however, you'll also find that the addition parser helpers reduce the amount of code you need to write by quite
a lot.

The setup is pretty easy. See `om-tutorial.core`:

```
(def parser (om/parser {:read   (p/new-read-entry-point local/read-local {:my-server remote/read-remote})
                        :mutate m/mutate}))
```

### Local Parsing

The parsing helper code in this example has been written with some care, with the intent to reduce the overall footprint 
of the application-specific local read code to a minimal level. The resulting helper functions are not tested
across a large amount of database structure, but they seem to work well so far and will be improved as 
I work on it. I've attempted to give you a way to read "the thing that should be here in the UI state", and
a way to "follow that ref". This reduces the total number of lines of client-specific read code for this example to
just 5 lines!

```
(defn read-local [env key params]
  (case key
    :ui/checked {:value (p/ui-attribute env key) } ; get a non-persistent UI bit of data
    :person/mate {:value (p/parse-join-with-reader read-local env key :limit 2)} ; to-one join, with recursion limit
    :people {:value (p/parse-join-with-reader read-local env key)} ; to-many join
    :widget {:value (p/parse-join-with-reader read-local env key :reset-depth 0)} ; to-one join, with recursion counter reset
    (p/db-value env key) ; just get the value that is at the "current location" in the database
    ))
```

Basically, you must use a "default" database format of Om, which basically means a normalized one where
anything with an Ident has been stuffed into root-level tables. The parsing helpers assume that the 
rest of your app state will follow the UI tree structure.

The parsing is fine if you want to use the special keyword `:missing` in place of data that will be demand loaded
later. The join processing code will naturally stop at such places when looking for local data.

### Separating UI-concern data from Persistent data

If you have a widget that has only client-local data, then just put it on that widget.

However, one trouble you'll likely run into pretty quickly is the fact that queries often complect local UI-concern data
with stuff that is stored on a server. If you try to stuff these transient values into the same locations and
do a remote fetch you'll either overwrite them, or you'll end up writing merge logic and plugging that in. Basically,
you'll make a mess.

Rather that fooling with that I've come up with a scheme where you can put any UI-specific data on a separate
app-state table as long as the component in question has an `Ident`. Since most things that are persistent can
(and probably will) have that, it seems like a non-intrusive requirement.

The parsing read helpers for local state then make it *look like* these UI attributes are on the persistent object,
when in fact they're pulled from a separate UI attribute table and merged in during the read.

### Basic read-local Rules

Write the `read-local` using a `case`. These are very fast. You may dispatch to a different read function as
you descend your state (by passing that function as the first argument to `parse-join-with-reader`, but you do not have to.

The *default* case should just be `(p/db-value env key)`. The trick is that the join parser will add `:db-path` to
the env, keeping track of where you are in the application state. The `db-value` function walks that path (following any
Om refs (e.g. `[:db/id 4]` it hits), and returns the value of `key` at that place in the state. Thus, if none of the
other keys make any noise about wanting to handle the read, it will try to get it from the current parse path.

Any UI cases that you've designed in should work by handing them off to `ui-attribute`.

Finally, any joins (singular or many) can be handled by the `parse-join-with-reader`. This descends the app state
at that key (following refs), and recursively processes the query. Since refs can create infinite loops,
it defaults to stopping recursion at a depth of 20. You can change this by using the `:limit n` parameter. Also,
the depth is tracked from the first join. If you want to track from the current join, use `:reset-depth 0`.

When objects are found, this combination will automatically filter out unwanted attributes. For example, if you
ask for `[:a]` in some sub-fragment, and the object in the state there has `{:a 1 :b 2}`, then this parser code
will return `{:a 1}`.

### Remote Fetch 

For each remote that you list in the reconciler (default is just `:remote`), the parser will run with `:target` set
in the env to that remote. This lets you gather up different sets of queries to send to each remote.

The reader factory I've created lets you supply a map from remote name to reader function, so that you can 
separate your logic out for each of these query parses.

In remote parsing mode, the parser expects you to return either `true` or a (possibly modified) AST node (which
comes in as `:ast` in `env`). Doing recursive parsing on this is a bit of a pain, but is also typically necessary
so that you can both maintain the structure of the query (which *must* be rooted from your Root component)
and prune out the bits you don't want.

The remote read in this example (so far) only wants a list of people. Everything else is client-local. Using the 
parsing helpers in the `om-tutorial.parsing` namespace, this pares down to this:

```
(defn read-remote [env key params]
  (case key
    :widget (p/recurse-remote env key true)
    :people (p/fetch-if-missing env key :make-root)
    :not-remote ; prune everything else from the parse
    )
  )
```

The `recurse-remote` function basically means "I have to include this node, because it is on the path to real
remote data, but it itself needs nothing from the server". The `fetch-if-missing` function has quite a bit 
of logic in it, but basically means "Everything from here down is valid to ask the server about".

The `:make-root` flag (which can be boolean or any other keyword, but only has an effect if it is `:make-root` or `true`)
is used to set up root processing. I'll cover that more later.

TODO: Elide keywords from the resulting fetch query if they are in the ui.* namespace, so we don't ask the server for them

### Server simulation

The present example has a server simulation (using a 1 second setTimeout). Hitting "refresh" will clear the `:people`, 
which will cause the remote logic to trigger. One second later you should see the simulated data I've placed on this
"in-browser server".

There is a lot more to do here, but tempids are not quite done yet, so I'll add more in as that becomes available.

### Mutation

The mutation story is based on multi-methods, dispatched by symbol. Any actions that you want to run locally just go
in the action thunk of the return of your mutation.

The UI-tree portion of your app state will end up with a bunch of refs in it, and the top-level of the state
will have Om-generated tables holding the objects themselves.

Here are some notes on the various kinds of operations you'll want to do:

- Create
    - Use `om/tempid` to get a temp id for the new object
    - Add the object to the top level table (e.g. `(swap! state update-in [:db/id ] assoc tmpid obj)`
    - Add refs (e.g. `[:db/id tmpid]`) to any UI components that should be showing that new object
    - Optionally return a remote ast to indicate you want to send the request to the server
- Read (covered in the read section)
- Update
    - Simply update the state in the "tables".
- Delete
    - Delete the refs from the UI state portion of state. You can leave the object in the tables (in case, say, 
      other UI components are looking at it, or you might want to undo the delete)

After doing a mutation, you can trigger re-renders by listing query bits after the mutation. Any keywords you list
will trigger re-renders of things that queried for those keywords. Any refs (e.g. `[:db/id 4]`) will trigger 
re-renders of anything that has that Ident. In the example below, anything that has included the prop named
`:widget` or has the Ident `[:db/id 4]` will re-render after the operation.

```
   (om/transact! this '[(app/do-thing) :widget [:db/id 4]])
```

## Devcard Tests!

Devcards supports tests. When you run this example
