# Minimal makefile for Cube server
#

# You can set these variables from the command line, and also
# from the environment for the first two.
CUBEOPTS    ?=
CUBEBUILD   ?= ant

build:
	@$(CUBEBUILD) build

.PHONY: build


deploy:
	@$(CUBEBUILD) deploy

.PHONY: deploy

# Catch-all target: route all unknown targets to Ant build.xml
# $(O) is meant as a shortcut for $(CUBEOPTS).
%:
	@$(CUBEBUILD) $@ $(CUBEOPTS) $(O)
