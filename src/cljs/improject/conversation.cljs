(ns improject.conversation
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]

            [improject.main :refer [friend-cell]]))

(defn conversation-page []
  (let [location (subscribe [:location])
        conv-partner (subscribe [:conversation-partner])
        me (subscribe [:user-model])]
    (fn []
      [:div
       [:div [:button#reset {:on-click #(dispatch [:reset-location :conversation])} "Reset"]
        [:header
         ;; well not exactly own, but that setting removes excess onhover- styling and the onclick listener
         [friend-cell @conv-partner :own? true]]
        [:article#conversation "Here be a conversation"]
        [:footer#message-input
         [:textarea {:placeholder "Write your stuff"}]
         [:button#send-msg "Send"]]]])))
