(ns sample1.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [sample1.ajax :as ajax]
    [ajax.core :refer [GET POST]]
    [reitit.core :as reitit]
    [clojure.string :as string])
  (:import goog.History))

(defonce session (r/atom {:page :home}))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page (:page @session)) "is-active")}
   title])

(defn navbar [] 
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "sample1"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span][:span][:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/about" "About" :about]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(def getAtom (r/atom 0)) ;Atom that will hold the value obtained from GET
(def postAtom (r/atom 0)) ;Atom that will hold the value obtained from POST
(def multAtom (r/atom 0)) ;Atom that will hold the value for multiplication

(def toAdd [(rand-int 1000) (rand-int 1000)])
;(def comps [(r/atom comp1 [1 2 0]) (r/atom comp2 [97 52 0]) (r/atom comp3 [2 2 0])])

(defn postSum [vec resultAtom]
  (POST "/api/math/plus"    {:headers {"accept" "application/json"}
                             :params {:x (first vec) :y (second vec)}
                             :handler #(reset! resultAtom (:total %))})
  )

(defn getSum [vec resultAtom]
  (GET "/api/math/plus"    {:headers {"accept" "application/json"}
                                         :params {:x (first vec) :y (second vec)}
                                         :handler #(reset! resultAtom (:total %))}

    )
  )

(defn postProduct [vec resultAtom]
  (POST "/api/math/times" {:headers {"accept" "application/json"}
                           :params  {:x (first vec) :y (second vec)}
                           :handler #(reset! resultAtom (:total %))}))

(defn home-page []

  [:p [:p "Using GET, the sum of " (first toAdd) " and " (second toAdd) " is: " @getAtom]
   [:p "Using POST, the sum of " (first toAdd) " and " (second toAdd) " is: " @postAtom]
   [:p "The product of " (first toAdd) " and " (second toAdd) " is " @multAtom]]

  )

(def pages
  {:home #'home-page
   :about #'about-page})

(defn page []
  [(pages (:page @session))])

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/" :home]
     ["/about" :about]]))

(defn match-route [uri]
  (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
       (reitit/match-by-path router)
       :data
       :name))
;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [^js/Event.token event]
        (swap! session assoc :page (match-route (.-token event)))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(swap! session assoc :docs %)}))

(defn ^:dev/after-load mount-components []
  (rdom/render [#'navbar] (.getElementById js/document "navbar"))
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []

  (ajax/load-interceptors!)
  (fetch-docs!)

  (postSum toAdd postAtom)
  (getSum toAdd getAtom)
  (postProduct toAdd multAtom)

  (hook-browser-navigation!)
  (mount-components))
