# subs-dead-letter-notifier
[![Build Status](https://travis-ci.org/EMBL-EBI-SUBS/subs-dead-letter-notifier.svg?branch=master)](https://travis-ci.org/EMBL-EBI-SUBS/subs-dead-letter-notifier)

The purpose of this application is sending a notification, in our case an email, if one of our services could not process a message that it has received.

## The workflow of the application

When a message is not processable by one of our services, then it gets sent to a so called Dead Letter Exchange in RabbitMQ.
Currently there are 2 queues has been set up and they bound to the Dead Letter Exchange with a routing key of '#', that means both of them will gets all of the messages sent to this Dead Letter Exchange.
One queue is for debugging purposes and another application will take care of (consume) it, the other one is what this application is consuming.

This application will read every message from that queue and store it in an in memory map, where the key would be the routing key of the message and the value would be the message body itself.
In the map it would store only one message by a routing key, because that is enough for investigating the issue/problem we are facing. We also don't want to generate a huge map and later a file containing similar or identical issues risking to trigger an `OutOfMemoryError`.

The application would send an email after a configurable amount of time if any event has occurred (any message has arrived).
This email would contains the number of problems happened in that period of time and an attachment containing the routing key and the body of the messages.  

## Configuration

The application can be configure via the `application.yml` file under the `src/main/resources` folder.

The application's settings can be found under the `dlqEmailer` and `spring.mail` keys.
Under the `dlqEmailer` key the `from`, `to` and `replyTo` keys should be changed to the relevant values.
Under the `spring.mail` key the `host` and `port` should be set to according to the company's smtp settings.

## License
This project is licensed under the Apache 2.0 License - see the [LICENSE.md](LICENSE.md) file for details.
