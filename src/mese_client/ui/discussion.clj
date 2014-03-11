(ns mese-client.ui.discussion
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [seesaw.border :as border]
            [mese-test.util :refer [seq-in-seq? in?]]
            [merpg.2D.core :as c]
            [mese-client.communications :refer :all]))

(comment
            +-------------------------------------------------------+
            |   Your friend's name                                  |
            |   Friend's personal msg                               |
            +-------------------------------------------------------+
            | Your fri- |                                           |
            | end's img |                                           |
            +-----------+                                           |
            |           |                                           |
            |           |                                           |
            |           |       Discussion                          |
            |           |                                           |
            |           |                                           |
            |           +-------------------------------------------+
            |           |                                           |
            +-----------+                            -----------    |
            |           |                           (    SEND   )   |
            |           |                            -----------    |
            | Your pers-|        Your msg                           |
            | onal img  |                                           |
            |           |                                           |
            +-----------+-------------------------------------------+)
