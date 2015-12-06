pages:
	lein with-profile prod cljsbuild once pages
	git checkout gh-pages
	cp resources/public/pages/tutorial.js .
	git add .
