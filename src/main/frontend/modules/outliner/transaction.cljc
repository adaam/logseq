(ns frontend.modules.outliner.transaction
  #?(:cljs (:require-macros [frontend.modules.outliner.transaction])))

(defmacro transact!
  "Batch all the transactions in `body` to a single transaction, Support nested transact! calls.
  Currently there are no options, it'll execute body and collect all transaction data generated by body.
  `Args`:
    `opts`: Every key is optional, opts except `additional-tx` will be transacted as `tx-meta`.
            {:graph \"Which graph will be transacted to\"
             :outliner-op \"For example, :save-block, :insert-blocks, etc. \"
             :additional-tx \"Additional tx data that can be bundled together
                              with the body in this macro.\"}
  `Example`:
  (transact! {:graph \"test\"}
    (insert-blocks! ...)
    ;; do something
    (move-blocks! ...)
    (delete-blocks! ...))"
  [opts & body]
  (assert (map? opts))
  `(if (some? frontend.modules.outliner.core/*transaction-data*)
     (do ~@body)
     (binding [frontend.modules.outliner.core/*transaction-data* (transient [])]
       ~@body
       (let [r# (persistent! frontend.modules.outliner.core/*transaction-data*)
             tx# (mapcat :tx-data r#)
             ;; FIXME: should we merge all the tx-meta?
             tx-meta# (first (map :tx-meta r#))
             all-tx# (concat tx# (:additional-tx ~opts))
             opts# (merge (dissoc ~opts :additional-tx) tx-meta#)]
         (when (seq all-tx#)
           (let [result# (frontend.modules.outliner.datascript/transact! all-tx# opts#)]
             {:tx-report result#
              :tx-data all-tx#
              :tx-meta tx-meta#}))))))
