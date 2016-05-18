#IM-project

##What is this? 
An instant messenger running in browser. Has standard one-to-one conversations, user profiles with similar data to old MSN Messenger, friendships and such. Not yet complete, but the basic conversations sort-of kind-of work so well that it pays off to write a better readme.

##Why is this?
Aside from being a buggy POS, Windows Live Messenger was the bestest IM there is (not counting stuff like irc). But, bugs were a problem, and my use case could've benefitted from having a script evaluator hooked on the message field. It could've been possible to extend the client, I dunno, but Microsoft's stuff usually is a royal PITA to write plugins for. Plus, back then I didn't know lisp. Which means I'd have tried to write a js-evaluator or something if I cared to.

##History
After learning everything I currently know, I knew IM hooked to Emacs would be golden. But if I wanted users, I'd have to start with a lousier^W easier-to-use interface. Thus I wrote a prototype IM server in Clojure (the lisp I know best) and a client with Seesaw. It was a prototype, and it was broken.

I let it rot for a couple of years in this repo. Now as an exercise to learn cljs I rewrote it, server with a less shitty api/architecture and client with ClojureScript instead of seesaw. Seesaw client miiiight follow if this gains traction. It's easier to script localhost when outside of the browser's sandbox. Also Emacs client is a possibility if I ever want to learn enough elisp.

##Where is this?
[http://mese-test.herokuapp.com](http://mese-test.herokuapp.com). You can register and log in there. It's still in-dev, thus finer details like editing your displayname after registration aren't yet implemented. I can be found there by the name of 'feuer'.