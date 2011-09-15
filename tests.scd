// --testTransmitter TCP

s.options.protocol = \tcp;
s.reboot;
s.addr.connect;
s.quit;

// --testTransmitter UDP

s.options.protocol = \udp;
s.reboot;

// --testReceiver

n = NetAddr( "localhost", 21327 );
//n = NetAddr( "127.0.0.1", 21327 );
n.sendMsg( "/hallo", 12, 3.4 );
n.sendBundle( 0.0, [ "/welt", 5.6, 78 ]);
n.sendMsg( "/quit" );