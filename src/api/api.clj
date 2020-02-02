(ns api.api
  (:require
   [muuntaja.core :as m]
   [reitit.coercion.spec :as rcs]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.spec :as rs]
   [ring.adapter.jetty :as jetty])
  (:import
   [org.eclipse.jetty.server Server]))

(defn jetty-start
  [handler opts]
  (jetty/run-jetty handler (-> opts (assoc :join? false))))

(defn jetty-stop
  [^Server server]
  (.stop server) ;; stop is async
  (.join server)) ;; so let's make sure it's really stopped!

(defn exception-handler
  [message exception request]
  {:status 500
   :body {:message message
          :exception (.getClass exception)
          :data (ex-data exception)
          :uri (:uri request)}})

(def exception-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {::infrastructure (partial exception-handler "infrastructure")})))

(defn foo
  [_]
  (throw (ex-info "Foo" {:type ::infrastructure})))

(defn router
  []
  (ring/router
   ["/api"
    ["/ping" {:get (fn [_] {:status 200 :body "Pong!"})}]
    ["/foo" {:get {:handler foo}}]]
   {:validate rs/validate
    :data {:coercion rcs/coercion
           :muuntaja m/instance
           :middleware [muuntaja/format-middleware
                        exception-middleware
                        parameters/parameters-middleware
                        coercion/coerce-exceptions-middleware
                        coercion/coerce-request-middleware
                        coercion/coerce-response-middleware]}}))

(defn ring-handler
  []
  (ring/ring-handler (router)
                     (ring/routes
                      (ring/create-default-handler))))
