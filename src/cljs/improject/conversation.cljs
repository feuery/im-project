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

(defn message-view [param]
  (let [{message :message
         {color :color
          underline? :underline
          bold? :bold
          italic? :italic
          font :font_name
          name :displayname} :sender
         date :date
         recipient :recipient} param]
    [:div
     [:p (str name " said at (" date "): ")]
     [:p {:style {:color color
                  :text-decoration (if underline? "underline" "none")
                  :font-weight (if bold? "bold" "none")
                  :font-style (if italic? "italic" "none")
                  :font-family (str font ", sans-serif")}}
      message]]))

(defn conversation-page []
  (let [location (subscribe [:location])
        conv-partner (subscribe [:conversation-partner])
        me (subscribe [:user-model])
        conversation (subscribe [:conversation])
        message (r/atom "")]
    (fn []
      (try
          ;; (js/alert (pr-str article))
        [:div
         [:div [:button#reset {:on-click #(dispatch [:reset-location :conversation])} "Reset"]
          [:header
           ;; well not exactly own, but that setting removes excess onhover- styling and the onclick listener
           [friend-cell @conv-partner :own? true]]

          (->> @conversation
               vec
               (map message-view)
               (into [:article#conversation]))
          
          [:footer#message-input
           [:textarea#message {:placeholder "Write your stuff"
                               :on-change #(reset! message (value-of %))
                               :on-key-down #(when (return-clicked? %)
                                               (dispatch [:send-message (:username @conv-partner) @message])
                                               (set! (.-value (js/document.getElementById "message")) "")
                                               (reset! message ""))}]
           [:button#send-msg "Send" ]]]]
        (catch :default e
          (js/alert e)
          (throw e))))))
