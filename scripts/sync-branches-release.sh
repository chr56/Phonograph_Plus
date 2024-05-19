#! /bin/bash
#
#  Copyright (c) 2022~2024 chr_56
#

REMOTES="github codeberg bitbucket github-organization"
BANCHES="main release preview"


# rebase
for branch in $BANCHES
do

git checkout -f $branch
git rebase dev

done

git checkout -f dev

# push
for R in $REMOTES
do

git push $R $BANCHES

done