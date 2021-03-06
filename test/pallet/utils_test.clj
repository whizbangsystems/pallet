(ns pallet.utils-test
  (:use pallet.utils)
  (:use clojure.test
        clojure.contrib.logging
        pallet.test-utils))

(defn- unload-ns
  "Hack to unload a namespace - remove-ns does not remove it from *loaded-libs*"
  [sym]
  (remove-ns sym)
  (sync []
   (alter (var-get #'clojure.core/*loaded-libs*) disj sym)))

(deftest find-var-with-require-test
  (testing "symbol with namespace"
    (unload-ns 'pallet.utils-test-ns)
    (Thread/sleep 1000)
    (is (= 1 (find-var-with-require 'pallet.utils-test-ns/sym-1)))
    (is (= 1 @(find-var-with-require 'pallet.utils-test-ns/sym-atom)))
    (testing "no reload of existing ns"
      (swap! (find-var-with-require 'pallet.utils-test-ns/sym-atom) inc)
      (is (= 2 @(find-var-with-require 'pallet.utils-test-ns/sym-atom)))))
  (testing "symbol and namespace"
    (unload-ns 'pallet.utils-test-ns)
    (Thread/sleep 1000)
    (is (= 1 (find-var-with-require 'pallet.utils-test-ns 'sym-1)))
    (is (= 1 @(find-var-with-require 'pallet.utils-test-ns 'sym-atom)))
    (testing "no reload of existing ns"
      (swap! (find-var-with-require 'pallet.utils-test-ns 'sym-atom) inc)
      (is (= 2 @(find-var-with-require 'pallet.utils-test-ns 'sym-atom))))))

(deftest make-user-test
  (let [username "userfred"
        password "pw"
        private-key-path "pri"
        public-key-path "pub"
        passphrase "key-passphrase"]
    (is (= {:username username
            :password password
            :private-key-path private-key-path
            :public-key-path public-key-path
            :passphrase passphrase
            :sudo-password password
            :no-sudo nil}
          (into {} (make-user username
                     :password password
                     :private-key-path private-key-path
                     :public-key-path public-key-path
                     :passphrase passphrase))))
    (is (= {:username username
            :password nil
            :private-key-path (default-private-key-path)
            :public-key-path (default-public-key-path)
            :passphrase nil
            :sudo-password nil
            :no-sudo nil}
           (into {} (make-user username))))
    (is (= {:username username
            :password nil
            :private-key-path (default-private-key-path)
            :public-key-path (default-public-key-path)
            :passphrase nil
            :sudo-password password
            :no-sudo nil}
           (into {} (make-user username :sudo-password password))))
    (is (= {:username username
            :password nil
            :private-key-path (default-private-key-path)
            :public-key-path (default-public-key-path)
            :passphrase nil
            :sudo-password nil
            :no-sudo true}
           (into {} (make-user username :no-sudo true))))))

(in-ns 'pallet.config)
(def admin-user (pallet.utils/make-user "fred"))
(in-ns 'pallet.utils-test)

(deftest admin-user-from-config-var-test
  (let [admin-user (admin-user-from-config-var)]
    (is (= "fred" (:username admin-user)))))

(deftest admin-user-from-config-test
  (let [admin-user (admin-user-from-config {:admin-user {:username "fred"}})]
    (is (= "fred" (:username admin-user)))))
