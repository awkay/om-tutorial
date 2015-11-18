# Remote Simulation Code

This is a handy little bit of code for playing with Om remotes. It
allows you to quickly simulate what your remote returns by simply
calling the send's callback after a 1s timeout.

This allows you to see you optimistic changes can work, along with how you might
go about getting the "real state" on the server after a mutation (e.g. forced
reads).

The example will also demonstrate how you might go about making sure your
forced read makes sense to your server (and structurally, makes sense to the
callback).
