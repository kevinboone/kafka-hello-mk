#!/bin/sh

#
# Script to run kafka-hello in "send" mode, using connection attributes
# in mk.env in the current directory.
#

. ./mk.env

java -jar target/kafka-hello-mk-0.0.1-jar-with-dependencies.jar -s -b $BOOTSTRAP -u $CLIENT_ID -p $CLIENT_SECRET -t foo "$@"
