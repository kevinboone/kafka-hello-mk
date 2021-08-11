# kafka-hello-mk

Version 0.0.1, August 2021

## What is this?

`kafka-hello-mk` is a trivial Java Kafka command-line 
client that uses security and 
authentication settings appropriate for Red Hat's Managed Kafka
service (RHOSAK).

It is the simplest self-contained Java example I could come up with, that
sends and/or receives a specific number of messages to/from a Kafka topic.

I wrote this sample specifically to demonstrate how to use a Java client
with Managed Kafka. I've included brief set-up instructions for the
Managed Kafka cluster at the end of this document.

## Building

    $ mvn package

This generates a JAR file in `target/' which is completely self-contained
(apart from the JVM, of course).

## Usage

    $ java -jar target/kafka-hello-mk-x.x.x-jar-with-dependencies {options}

There are a number of command-line switches, some of which are mandatory.
You must specify the bootstrap URI (`--bootstrap`) and
both of `--user` and `--password`, since user/password
is the only form of authentication currently provided. See
below for how to get this information from Managed Kafka.
You must also
specify one of `--send` or `--receive` to set the operating mode.
All the other switches
have defaults. To see a full list, use the `--help` switch.

There are example shell scripts `send.sh` and `recv.sh` that give
sample command lines. These read their settings from the file
`mk.env`, for which a sample is also provided.

## Logging

Modify the `log4j` configuration in `src/main/resources` to change 
the logging levels.

## Setting up Managed Kafka

You'll need an account on the Red Hat Managed Kafka service, and the
`rhoas` command-line tool (version 0.27.0 or later). If you have a 
version of `rhoas` from the Managed Kafka public demo and haven't updated
since, it won't work. 

Log in to Managed Kafka:

    $ rhoas login

If necessary, create a new Kafka cluster:

    $ rhoas kafka create

You'll be prompted for the region, etc. Create a service account if you 
haven't already -- that
is, specify user credentials for the Kafka service:

    $ rhoas service-account create

You'll be prompted for a place and format to store the credentials, which
will be machine-generated. It
doesn't matter what format you choose, as you'll need to read the file
and then use the credentials on the command line.

Create a new topic, for example:

    $ rhoas kafka topic create my-topic 

You can create a particular retention policy, etc., at this stage if
necessary.

Get the Kafka bootstrap URL for the client:

    $ kafka status

## Notes

`kafka-hello-mk` publishes Kafka messages with an integer key with value
"1". At present this isn't configurable, but that could easily be
changed in the source. The key affects how messages are distributed to
partitions in a multi-partition topic.

In 'receive' mode, the application might read more messages than the
value set using the `--num` switch. This is because it uses a time-based
poll, and won't throw away messages that arrived during the poll period, 
even if the message count exceeds the specified value. 

It isn't possible to set the consumer group ID at the command line although,
again, it would be easy to add this facility. The group ID is relevant
when using multiple consumers on the same Kafka topic.

The Managed Kafka service uses certificates signed by a certificate authority
that is generally recognized by Java. It shouldn't be necessary to create a 
custom trust store, or import any additional certificates.

With the Kafka client library version specified in `pom.xml`, any Java
JVM with version 1.8.0 or later should be fine.

There is no way at present to start receiving from any offset other than
the current position for the default consumer group. That is, you
can't rewind and read earlier messages. 


