{"12.2.2014" {:hours-spent 2 :done "Kirjoitettu blogiposti; Kerrattu Clojuren webbijuttujen perusteet"}
 "13.2.2014" {:hours-spent 2 :done "Tehty keskinkertainen pohja blogin kuvauksen mukaan. Outboxien automatiikka on suunnilleen koossa"}
 "16.2.2014-18.2.2014" {:hours-spent 4 :done "Yritetty toteuttaa autentikointia. Tajuttu ettei sessionid:tä oikeasti tarvitsekaan mihinkään"}
 "19.2.2014-20.2.2014" {:hours-spent 7 :done "Login bugaa jännästi. Macillä timeout tapahtuu, kun mikään ei päivitä viimeisemmän kutsun ajankohtaa, mutta Archilla kaikki toimii. Lisäksi toteutettu sessiot ip-osoitteilla, toteutettu /send-msg/ - polku, ja havaittu tämä jännä hajoaminen macillä"}
 "21.2.2014" {:hours-spent 5 :done "Sessionid palautettu, koska jokaisella clientillä ei ole omaa julkista IP:tä. Vahvistettu viestien kulkeminen palvelinpäässä, ja riidelty session-authenticatesissa sen faktan kanssa että \"1\" != 1"}
 "22.2.2014-aamu" {:hours-spent 2 :done "Yritetty toteuttaa inboxien dumppaamista asiakkaalle"}
 "23.2.2014" {:hours-spent 5 :done "Toteutettu inboxien dumppaus, ja korjattu viestien reititys git-commitin 4748edc mukaan"}}
