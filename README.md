# TCP-Protocol 
To run the project :  

```
java Receiver
java Sender
```
Selective Repeat and Go-back-N simulator: http://www.ccs-labs.org/teaching/rn/animations/gbn_sr/  
To kill processes listening on a particular port enter : lsof -n -i4TCP:[PORT] | grep LISTEN | awk '{ print $2 }' | xargs kill  


