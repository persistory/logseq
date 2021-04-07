(ns frontend.modules.outliner.datascript
  #?(:clj (:require [clojure.core :as core]))
  #?(:cljs (:require-macros [frontend.modules.outliner.datascript]))
  #?(:cljs (:require [datascript.core :as d]
                     [frontend.db.conn :as conn]
                     [frontend.modules.outliner.pipeline :as pipelines])))

#?(:cljs
   (defn new-outliner-txs-state [] (atom [])))

#?(:cljs
   (defn outliner-txs-state?
     [state]
     (and
       (instance? cljs.core/Atom state)
       (vector? @state))))

#?(:cljs
   (defn add-txs
     [state txs]
     (assert (outliner-txs-state? state)
       "db should be satisfied outliner-tx-state?")
     (swap! state into txs)))

#?(:cljs
   (defn transact!
     [txs]
     (when (seq txs)
       (let [conn (conn/get-conn false)
             rs (d/transact! conn txs)]
         (pipelines/after-transact-pipelines rs)
         rs))))

#?(:clj
   (defmacro auto-transact!
     "Copy from with-open.
     Automatically transact! after executing the body."
     [bindings & body]
     (#'core/assert-args
       (vector? bindings) "a vector for its binding"
       (even? (count bindings)) "an even number of forms in binding vector")
     (cond
       (= (count bindings) 0) `(do ~@body)
       (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                                 (try
                                   (auto-transact! ~(subvec bindings 2) ~@body)
                                   (transact! (deref ~(bindings 0)))))
       :else (throw (IllegalArgumentException.
                      "with-db only allows Symbols in bindings")))))