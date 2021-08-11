#!/bin/sh

#
# Script to run kafka-hello in "receive" mode, using connection attributes
# in mk.env in the current directory.
#

. ./mk.env

java -jar target/kafka-hello-mk-0.0.1-jar-with-dependencies.jar -r -b $BOOTSTRAP -u $CLIENT_ID -p $CLIENT_SECRET -t foo "$@"
