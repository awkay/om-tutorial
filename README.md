# Om Tutorial Code

This is a project holding an Om (next) project that is to be used
as the basis of a complete overview of the features of Om. For 
simplicity, the server-side component is simulated in the browser. 
The code you would write on the server is identical (though you 
will need some minor plumbing to do the actual network bits).

## What's inside?

The organization is in flux. The top-level `src` directory contains:

- `main`: A demo application with simulated remote server (in progress, but working)
- `tutorial`: Devcards with a live tutorial (in progress)
- `cards` : Some development/tests written in devcards

## Running it

There is a clojure script in the `script` folder. Simply run that in Cursive (Run..., Add a Clojure Local REPL, Run with Clojure Main (NOT nREPL!))
or at the command line with:

```
lein run -m clojure.main
```

at that REPL, run:

```
user=> (start-dev)
```

Then browse to all of the following URLs in separate tabs:

```
http://localhost:3450/tutorial.html   - Devcards-based Tutorial (start here)
http://localhost:3450/                - Main app
http://localhost:3450/cards.html      - Devcards (tests) UI
```

I have it set up this way because later I'm going to add support for running both an nREPL and fighweel REPL
from the same JVM, and this makes it easier to do. You can edit the `src/dev/user.clj` if you like, and put
`(start-dev)` at the bottom to avoid having to manually start it.

For now, feel free to browse the source of the demo app, or follow the pages in the tutorial.

Expect things to change often...this is a work in progress that may take several weeks. If you'd like
to contribute, I'd love the help. Ping me on Slack (@tony.kay) so we don't stomp on each other.

Some things I'd like to have (but don't have the time to get to):

- Docs in the UI section (or a subsection) that talk about more basics of (stateless) UI. E.g. more references
to React docs, what the `#js` stuff is about and why you only do it on dom elements. Basic gotchas for 
beginners. Bonus points if anyone wants to hack the cljs ecosystem so that code pretty-printing can _handle_
`#js`.
- A stateless Om component that can render a tree on demand (given the tree data in props).
  e.g.,  http://www.cssscript.com/creating-simple-diagrams-with-nodes-and-links-using-svg-and-d3-js/
- CSS for anything you think is too ugly

