this package collects all classes for creating schedule
to maintain sip connection all the time alive

1)  create pending intent, which periodically trigger broadcast intent
2)  intent receiver start reconnect service
3)  that service send registration request to the server