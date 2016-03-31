GRADLE		?= gradle
GIT		?= git

REPO_URL	:= $(shell $(GIT) config remote.origin.url)
COMMIT		:= $(shell $(GIT) rev-parse HEAD)

nothing:

gh-pages:
	$(GRADLE) javadoc
	rm -rf gh-pages
	mkdir gh-pages
	cp -r client/build/docs/javadoc gh-pages/client
	cp -r master/build/docs/javadoc gh-pages/master
	cd gh-pages && $(GIT) init
	cd gh-pages && $(GIT) add .
	cd gh-pages && $(GIT) commit -m "$(COMMIT)"
	cd gh-pages && $(GIT) push -f $(REPO_URL) master:gh-pages
	rm -rf gh-pages

.PHONY: nothing gh-pages
