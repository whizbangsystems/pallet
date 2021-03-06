(ns pallet.live-test.jclouds-live-test
  (:use clojure.test)
  (:require
   [pallet.live-test :as live-test]
   [pallet.core :as core]
   [pallet.resource :as resource]
   [pallet.compute :as compute]))

(deftest node-types-test
  (is (= {:repo {:tag :repo :base-tag :repo :image {:os-family :ubuntu}
                 :count 1 :phases {}}}
         (live-test/node-types
          {:repo {:image {:os-family :ubuntu}
                  :count 1
                  :phases {}}}))))

(deftest counts-test
  (let [specs {:repo {:image {:os-family :ubuntu}
                  :count 1
                  :phases {}}}]
    (is (= {{:tag :repo :base-tag :repo :image {:os-family :ubuntu}
             :count 1 :phases {}} 1}
           (#'live-test/counts specs)))))

(deftest build-nodes-test
  (let [specs {:repo {:image {:os-family :ubuntu}
                      :count 1
                      :phases {}}}
        service (compute/compute-service "stub" "" "")]
    (is (= 1
           (count
            (live-test/build-nodes
             service (live-test/node-types specs) specs))))))

(deftest live-test-test
  (live-test/set-service! (compute/compute-service "stub" "" ""))
  (live-test/with-live-tests
    (doseq [os-family [:centos]]
      (live-test/test-nodes
       [compute node-map node-types]
       {:repo {:image {:os-family os-family}
               :count 1
               :phases {}}}
       (let [node-list (compute/nodes compute)]
         (is (= 1 (count ((group-by compute/tag node-list) "repo")))))))
    ;; (is (= 0
    ;;        (count
    ;;         ((group-by compute/tag (compute/nodes @live-test/service))
    ;;          "repo"))))
    )
  (testing "with prefix"
    (live-test/with-live-tests
      (doseq [os-family [:centos]]
        (live-test/test-nodes
         [compute node-map node-types]
         {:repo {:image {:os-family os-family :prefix "1"}
                 :count 1
                 :phases {}}}
         (let [node-list (compute/nodes compute)]
           (is (= 1 (count ((group-by compute/tag node-list) "repo1")))))))
      ;; (is (= 0
      ;;        (count
      ;;         ((group-by compute/tag (compute/nodes @live-test/service))
      ;;          "repo1"))))
      )))
