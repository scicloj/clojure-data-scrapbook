(ns index
  (:require [clojure.string :as string]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]
            [vald-client-clj.core :as vald]
            [wkok.openai-clojure.api :as openai]
            [scicloj.kindly.v4.kind :as kind]))

(assert
 (System/getenv "OPENAI_API_KEY"))

;; # plan
;; * keep our knowledge in a vector database
;; * inject that into chats

;; # some Knowledge

(def a-wikipedia-paragraph-about-bananas
  "By wikipedia:
 The banana fruits develop from the banana heart, in a large hanging cluster, made up of tiers (called \"hands\"), with up to 20 fruit to a tier. The hanging cluster is known as a bunch, comprising 3–20 tiers, or commercially as a \"banana stem\", and can weigh 30–50 kilograms (66–110 lb). Individual banana fruits (commonly known as a banana or \"finger\") average 125 grams (4+1⁄2 oz), of which approximately 75% is water and 25% dry matter (nutrient table, lower right).")

(kind/md a-wikipedia-paragraph-about-bananas)

;; # Using openai-clojure

(defonce create-embedding
  (memoize openai/create-embedding))

(defonce create-chat-completion
  (memoize openai/create-chat-completion))

;; ## chat completions

(create-chat-completion
 {:model "gpt-3.5-turbo"
  :messages [{:role "user"
              :content "What is the average weight of a banana fruit?"}]})

;; ## chat completions based on some knowledge

(create-chat-completion
 {:model "gpt-3.5-turbo"
  :messages [{:role "system"
              :content a-wikipedia-paragraph-about-bananas}
             {:role "user"
              :content "What is the average weight of a banana fruit?"}]})

;; ## embeddings

(create-embedding
 {:model "text-embedding-ada-002"
  :input a-wikipedia-paragraph-about-bananas})

;; ## embeddings (cont.)

(defn ->embedding-vector [text]
  (-> {:model "text-embedding-ada-002"
       :input text}
      create-embedding
      :data
      first
      :embedding))

(-> a-wikipedia-paragraph-about-bananas
    ->embedding-vector)

;; ## embedding dimension

(-> a-wikipedia-paragraph-about-bananas
    ->embedding-vector
    count)

;; # Using vald-client-clj

(def embedding-dimension 1536)

(def vald-client
  (vald/vald-client "localhost" 8081))

;; ## Inserting vectors

(defn insert-vector [v id info]
  (-> vald-client
      (vald/insert {}
                   id
                   (vec v))))

;; ## Remembering vectors on our REPL

(defonce *vectors
  (atom {}))

(defn remember-vector! [v id info]
  (if-let [known-case (@*vectors id)]
    (do (assert (-> known-case
                    :vector
                    (= v)))
        :ok-already-there)
    ;; else
    (do (insert-vector v id info)
        (swap! *vectors assoc id {:info info
                                  :vector v})
        :ok-just-added)))

(def my-vector
  (-> embedding-dimension range reverse vec))

(remember-vector! my-vector
                  "my-id-6"
                  {:text "hello"})

;; ## Searching for vectors

(defn search [v options]
  (-> vald-client
      (vald/search (merge {:num 1}
                          options )
                   v)))

(-> my-vector
    (search {:num 3}))

;; # Managing knowledge

;; ## Collecting knowledge

(-> a-wikipedia-paragraph-about-bananas
    ->embedding-vector
    (remember-vector! "banana-1" {:text a-wikipedia-paragraph-about-bananas}))

;; ## Extracting vectors

(-> a-wikipedia-paragraph-about-bananas
    ->embedding-vector
    (search {}))

;; ## Extracting the relevant knowledge

(defn extract [text]
  (-> text
      ->embedding-vector
      (search {})
      first
      :id
      (@*vectors)))

(-> a-wikipedia-paragraph-about-bananas
    extract)

;; ## Extracting the relevant knowledge (cont.)

(-> "What is the average weight of a banana fruit?"
    extract
    :info)

;; # Connecting everything

(let [question "What is the average weight of a banana fruit?"]
  (create-chat-completion
   {:model "gpt-3.5-turbo"
    :messages [{:role "system"
                :content (-> question
                             extract
                             :info
                             :text)}
               {:role "user"
                :content question}]}))

;; # More examples

(defn random-vector [means]
  (-> means
      (fun/+ (dtype/make-reader :float32 (count means) (rand)))
      dtype/clone
      vec))

(comment

  (dotimes [i 4]
    (-> embedding-dimension
        range
        random-vector
        (remember-vector! (str "A" i) {}))
    (-> embedding-dimension
        range
        reverse
        dtype/->float-array
        random-vector
        (remember-vector! (str "B" i) {})))

  (-> vald-client
      (vald/search {:num 40}
                   (range embedding-dimension))))






(create-chat-completion
 {:model "gpt-3.5-turbo"
  :messages [{:role "user"
              :content "i am tired"}
             {:role "assistant"
              :content "negative"}
             {:role "user"
              :content "i am alive"}
             {:role "assistant"
              :content "positive"}
             {:role "user"
              :content "i am happy"}]})


(create-chat-completion
 {:model "gpt-3.5-turbo"
  :messages [{:role "system"
              :content "please answer in Spanish"}
             {:role "user"
              :content "what is clojure?"}]})
