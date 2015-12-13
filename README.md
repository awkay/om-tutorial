# Om Tutorial Code

This is a project holding an Om (next) project that is to be used
as the basis of a complete overview of the features of Om. For 
simplicity, the server-side component is simulated in the browser. 
The code you would write on the server is identical (though you 
will need some minor plumbing to do the actual network bits).

I try to keep it running at https://awkay.github.io/om-tutorial/, 
though it is really meant to be cloned locally, as the 
exercises have you edit the code.

## What's inside?

The organization is in flux. The top-level `src` directory contains:

- `tutorial`: Devcards with a live tutorial (in progress)
- `main`: A demo application with simulated remote server (in progress, but working with Om master or alpha25+)
- `cards` : Some development/tests written in devcards

The latter two are for my internal purposes at the moment, so you can largely ignore them.

## Running it

There is a clojure script in the `script` folder. You can run it with `lein` or in Cursive (Run..., Add a Clojure Local REPL, Run with Clojure Main (NOT nREPL!), parameters `script/figwheel.clj`)
or at the command line with:

```
lein run -m clojure.main script/figwheel.clj
```

Then browse to the following URL:

```
http://localhost:3450   Devcards-based Tutorial (start here)
```

Expect things to change often...this is a work in progress that may take several weeks or even months. If you'd like
to contribute, I'd love the help. Ping me on Slack (@tony.kay) so we don't stomp on each other.

Some things I'd like to have (but don't have the time to get to):

- Docs in the UI section (or a subsection) that talk about more basics of (stateless) UI. E.g. more references
to React docs, what the `#js` stuff is about and why you only do it on dom elements. Basic gotchas for 
beginners. Bonus points if anyone wants to hack the cljs ecosystem so that code pretty-printing can _handle_
`#js`.
- CSS for anything you think is too ugly

## Figwheel notes

Once the figwheel REPL is going, you can clean and rebuild with 

```
(reset-autobuild)
```

after which you probably want to reload the page in your browser to clear out any cruft.
