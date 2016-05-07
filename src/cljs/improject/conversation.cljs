(ns improject.conversation
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]

            [improject.main :refer [friend-cell]]
            [improject.formtools :refer [value-of value-of-checkbox return-clicked?]]))

(defn conversation-page []
  (let [location (subscribe [:location])
        conv-partner (subscribe [:conversation-partner])
        me (subscribe [:user-model])
        conversation (subscribe [:conversation])
        message (r/atom "")]
    (fn []
      [:div
       [:div [:button#reset {:on-click #(dispatch [:reset-location :conversation])} "Reset"]
        [:header
         ;; well not exactly own, but that setting removes excess onhover- styling and the onclick listener
         [friend-cell @conv-partner :own? true]]
        [:article#conversation  @conversation
         [:br] [:hr]
         @message
         [:br] [:hr]
         (pr-str @conv-partner)]
        [:footer#message-input
         [:textarea#message {:placeholder "Write your stuff"
                     :on-change #(reset! message (value-of %))
                     :on-key-down #(when (return-clicked? %)
                                     (dispatch [:send-message (:username @conv-partner) @message])
                                     (set! (.-value (js/document.getElementById "message")) "")
                                     (reset! message ""))}]
         [:button#send-msg "Send" ]]]])))
