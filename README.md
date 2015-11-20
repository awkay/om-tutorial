# Om Tutorial Code

This is a project holding an Om (next) project that is to be used
as the basis of a complete overview of the features of Om. For 
simplicity, the server-side component is simulated in the browser. 
The code you would write on the server is identical (though you 
will need some minor plumbing to do the actual network bits).

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

