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
lein run -m clojure.main
user=> (start-dev)
```

Then browse to all of the following URLs in separate tabs:

```
http://localhost:3450/tutorial.html   - Devcards-based Tutorial (start here)
http://localhost:3450/                - Main app
http://localhost:3450/cards.html      - Devcards (tests) UI
```

Mainly, you want to follow the pages in the tutorial. It will refer to the other tabs as it goes.


