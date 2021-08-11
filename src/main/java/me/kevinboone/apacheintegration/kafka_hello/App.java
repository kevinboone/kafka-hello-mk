/*===========================================================================

  kafka-hello-mk
  App.java

  Copyright (c)2021 Kevin Boone, distributed under the terms of the 
  GNU Public Licence v3.0

===========================================================================*/

package me.kevinboone.apacheintegration.kafka_hello;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import java.util.Properties; 
import java.util.Collections; 
import java.time.Duration;


public class App
  {
  // How long the consumer will wait for new messages, if none are
  //   available. Note that this is a pretty arbitrary figure in this 
  //   simple implementation, since we will repeat the message poll
  //   until the user-specified number of messages has been received.
  //   In a real application, this setting can affect efficiency.
  static final int CONSUMER_WAIT = 10; // Seconds

  // The consumer group ID. Arbitrary here but, in a real application, this
  //   ID will ensure that multiple consumers in the same group see different
  //   messages. That is, collaborating consumers will read from different
  //   offsets in the same partition.
  static final String GROUP_ID = "FooConsumer"; 

  /**
  Start here.
  */
  public static void main (String args[]) throws Exception
    {
    // Default message text to send
    String msg = "Hello, World!";
    // Message key (we use integer keys in this example)
    int key = 1; // TODO -- make this configurable
    // Number of messages to send or receive
    int numMsgs = 1;
    // "Send" mode
    boolean send = false; 
    // "Receive" mode
    boolean receive = false;
    // Default Kafka topic
    String topic = "my-topic";

    // Parse command line
    Options options = new Options();
    options.addOption ("b", "bootstrap", true, "bootstrap server URI");
    options.addOption ("p", "password", true, "password");
    options.addOption ("h", "help", false, "show this message");
    options.addOption ("m", "message", true, "message text");
    options.addOption ("n", "num", true, "number of messages");
    options.addOption ("r", "receive", false, "receive message");
    options.addOption ("s", "send", false, "send message");
    options.addOption ("t", "topic", true, "topic");
    options.addOption ("u", "user", true, "user");

    CommandLineParser parser = new GnuParser();
    CommandLine cl = parser.parse (options, args);

    // If user specified --help, show the help message and then exit
    if (cl.hasOption ("help"))
      {
      showHelp (options);
      System.exit (0);
      }

    if (cl.hasOption ("receive"))
      receive = true;
    if (cl.hasOption ("send"))
      send = true;

    // User must specify at least one of --send or --receive
    if (!send && !receive)
      {
      throw new Exception 
          ("One of '--send' or '--receive' must be specified");
      }

    String user = cl.getOptionValue ("u"); 
    if (user == null)
      throw new Exception 
        ("No user ID (client ID) specified -- use '--user'");

    String password = cl.getOptionValue ("p"); 
    if (password == null)
      throw new Exception 
        ("No password (client secret) specified -- use '--password'");

    String bootstrap = cl.getOptionValue ("b");
    if (bootstrap == null)
      throw new Exception 
        ("No bootstrap server specified -- use '--bootstrap'");

    String t = cl.getOptionValue ("t");
    if (t != null)
      topic = t;

    String n = cl.getOptionValue ("n");
    if (n != null)
      numMsgs = Integer.parseInt (n);

    String m = cl.getOptionValue ("m");
    if (m != null)
      msg = m;

    // Call doProduce or doConsume, according to which of --send or
    //   --receive was specified

    if (send)
      doProduce (bootstrap, topic, user, password, msg, key, numMsgs);

    if (receive)
      doConsume (bootstrap, topic, user, password, numMsgs);
    }


  /**
  Create a KafkaConsumer and read numMsgs messages. Note that the
  method might read more messages, if they are available, because of
  the way Kafka polling works. We won't throw away perfectly good
  messages, if they have actually been read into the client.
  */
  static void doConsume (String bootstrap, String topic,
        String user, String password, int numMsgs)
        throws Exception
    {
    Properties consumerProps = new Properties();
    consumerProps.put (ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, 
      bootstrap);
    consumerProps.put (ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
    consumerProps.put (ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
      IntegerDeserializer.class.getName());
    consumerProps.put (ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
      StringDeserializer.class.getName());
    // Security properties are common to producer and consumer
    setSecurityProps (consumerProps, user, password);

    KafkaConsumer<Integer, String> consumer = 
      new KafkaConsumer<> (consumerProps);

    consumer.subscribe (Collections.singletonList (topic));

    // Poll for messages, until the user-specified total (deflt 1)
    //   has been reached. Note that each poll() may receive multiple
    //   messages, if they are available.
    int received = 0;
    while (received < numMsgs)
      {
      ConsumerRecords<Integer, String> records = consumer.poll
        (Duration.ofSeconds (CONSUMER_WAIT)); 

      for (ConsumerRecord<Integer, String> record : records) 
        {
        System.out.println("Received message: (" 
            + record.key() + ", " 
            + record.value() + ") at offset " 
            + record.offset());
        received++;
        }
      }
    consumer.close();
    }


  /** 
  Create a KafkaProducer and send numMsgs messages. 
  */
  static void doProduce (String bootstrap, String topic,
        String user, String password, String msg, int key, int numMsgs)
        throws Exception
    {
    Properties producerProps = new Properties();
    producerProps.put (ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
    producerProps.put (ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
        IntegerSerializer.class.getName());
    producerProps.put (ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
        StringSerializer.class.getName());
    // Security properties are common to producer and consumer
    setSecurityProps (producerProps, user, password);
    
    // Send the messages. Note that each is identical in this simple
    //   implementation
    KafkaProducer<Integer, String> producer = 
        new KafkaProducer<> (producerProps);
    for (int i = 0; i < numMsgs; i++)
      producer.send (new ProducerRecord<> (topic, key, msg)).get();
    producer.close();
    }


  /**
  Prints the command-line arguments.
  */
  static void showHelp (Options options)
    {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp ("kafka-hello", options );
    System.out.println ("One of --send or --receive must be given, and ");
    System.out.println ("both of --user and --password. All other switches ");
    System.out.println ("are optional.");
    }


  /** 
  Sets the authentication properties. These settings are common to producer
  and consumer, so it simplifies things just a little to put them in
  a single method. Note that the SASL settings are appropriate for Red
  Hat Managed Kafka, but perhaps not for other installations.
  */
  static void setSecurityProps (Properties props, String user, String password)
    {
    props.put ("security.protocol", "SASL_SSL");
    props.put ("sasl.mechanism", "PLAIN");
    props.put ("sasl.jaas.config", 
         "org.apache.kafka.common.security.scram.ScramLoginModule " 
         + "required username=\"" + user 
         + "\" password=\"" + password + "\";");
    }


  }


