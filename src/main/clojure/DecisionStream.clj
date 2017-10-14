(ns DecisionStream
    (:import java.io.FileReader
      au.com.bytecode.opencsv.CSVReader
      org.apache.commons.math3.stat.inference.MannWhitneyUTest
      org.jblas.DoubleMatrix
      edu.decision.stream.MatrixUtil
      java.util.Arrays
      java.lang.reflect.Array)
  (:gen-class))

(declare ^:dynamic pLevel)
(declare ^:dynamic mgLevel)
(declare ^:dynamic baseDir)
(declare ^:dynamic trainX)
(declare ^:dynamic trainY)
(declare ^:dynamic testDt)
(declare ^:dynamic testRes)
(declare ^:dynamic classification)
(def ^:dynamic verbose true)

(defn prt [& vs] (if verbose (apply println vs)))
(defn average [vs] (/ (apply + vs) (count vs)))
(def mt (MannWhitneyUTest.))
(defn mapValues [f m] (zipmap (keys m) (map f (vals m))))

(defn labelAr [m] (MatrixUtil/getData (.getRow m 0)))

(defn pVAr [a b] (let [aSize (Array/getLength a)
                       bSize (Array/getLength b)]
                      (if (or (= 0 aSize) (= 0 bSize))
                        1 (if (or (> 2 aSize) (> 2 bSize)) (.mannWhitneyUTest mt a b) (MatrixUtil/kolmogorovSmirnovTest a b)))))

(defn limId [sortedFeature threshold]
      (let [lId (MatrixUtil/indexOf sortedFeature threshold)]
           [(/ (+ (Array/get sortedFeature lId) (Array/get sortedFeature (- lId 1))) 2) lId]))

(defn calcP [sortedLabel [threshold lId]] [(pVAr (Arrays/copyOfRange ^doubles sortedLabel 0 (int lId))
                                                 (Arrays/copyOfRange ^doubles sortedLabel (int lId) (Array/getLength sortedLabel))) threshold])

(defn bestThreshold [X sections fId]
      (let [feature (.getRow X fId)
            sortedIds (.sortingPermutation feature)
            sortedFeature (MatrixUtil/getData (.getColumns feature sortedIds))
            sortedLabel (MatrixUtil/getData (.get X 0 sortedIds))
            minV (first sortedFeature)
            maxV (last sortedFeature)
            step (/ (- maxV minV) sections)
            thresholds (range maxV minV (- step))
            lIds (reduce (fn [mp [thr lId]]
                             (let [lId1 (get mp thr)] (if-not lId1 (assoc mp thr lId) (assoc mp thr (max lId lId1)))))
                         {} (map (partial limId sortedFeature) thresholds))
            thrPart (pmap (partial calcP sortedLabel) lIds)]
           (if (empty? thrPart) thrPart (conj (apply min-key first thrPart) fId))))

(defn bestSplit [X fN]
      (let [sections (int (Math/sqrt (.getColumns X)))
            bestThrds (pmap (partial bestThreshold X sections) (range 1 (.getRows X)))
            filtd (remove (fn [el] (nil? (second el))) bestThrds)]
           (if (empty? filtd) filtd (apply min-key first filtd))))

(defn mergePair [a b]
      (let [mtr (DoubleMatrix/concatHorizontally (first a) (first b))]
           [[mtr (into (second a) (second b)) (labelAr mtr) false]]))

(defn mergeAll [nds limit] (loop [mps (sort-by (fn [mp] (.getColumns (first mp))) < nds)]
                                 (let [n (count mps)
                                       merged (loop [l1 mps res []]
                                                    (if (> 2 (count l1))
                                                      (into res l1)
                                                      (let [mp1 (first l1)
                                                            l2 (next l1)
                                                            [p mp2] (reduce
                                                                      (fn [[pX mpX] mpY]
                                                                          (let [pY (pVAr (nth mpY 2) (nth mp1 2))]
                                                                               (if (> pY pX) [pY mpY] [pX mpX]))) [0 nil] l2)
                                                            l3 (if (> p limit) (remove (fn [ll] (= ll mp2)) l2) l2)
                                                            r3 (into res (if (> p limit) (mergePair mp1 mp2) [mp1]))]
                                                           (recur l3 r3))))]
                                      (if (= n (count merged)) merged (recur merged)))))

(defn mergeNds [nds] (let [mgd (mergeAll nds (mgLevel))] (prt "merged" (count mgd) "from" (count nds)) mgd))

(defn- giniImpurity [X]
       (let [n (.getColumns X)
             y (labelAr X)
             labelCounts (frequencies y)
             label-freq (mapValues #(/ % n) labelCounts)
             labels (set (keys labelCounts))
             label-probs (map #(* (label-freq %) (label-freq %)) labels)
             gini (- 1.0 (reduce + label-probs))]
            gini))

(defn trainDStream
      ([X rMap]
        (let [root (atom nil)]
             (trainDStream rMap [[X [root] (labelAr X) false]] 1 0 (.getColumns X) 0 Double/MAX_VALUE 1)
             (deref root)))
      ([rMap inNodes impurity bedRes featN failsN bestImpurity cycle]
        (if (not-empty (remove last inNodes))
          (let [nodesInf (mergeNds inNodes)
                setNds (fn [chd pAtoms] (dorun (map (fn [pA] (reset! pA chd)) pAtoms)))
                [leaves nodes] ((juxt filter remove) last nodesInf)
                _ (prt "N" cycle "|" (count nodes) "nonterminal nodes" "|" (count leaves) "leaves")
                newImpurity (apply + (map (fn [[X pAtoms]] (* (giniImpurity X) (/ (.getColumns X) featN))) inNodes))
                _ (prt "Cross-node Gini impurity" (double newImpurity))
                nds (into leaves
                          (mapcat
                            (fn [[X pAtoms Y]]
                                (let [splt (bestSplit X featN)]
                                     (if (and (not (empty? splt)) (< failsN (Math/sqrt (.getRows X))) (> (pLevel) (first splt)))
                                       (let [[_ threshold fId] splt
                                             sortedIds (.sortingPermutation (.getRow X fId))
                                             sortedX (.getColumns X sortedIds)
                                             sortedFtr (MatrixUtil/getData (.getRow sortedX fId))
                                             labelId (MatrixUtil/indexOf sortedFtr threshold)
                                             leftA (atom nil)
                                             rightA (atom nil)
                                             rowN (.getRows sortedX)
                                             left (.getRange sortedX 0 rowN 0 labelId)
                                             right (.getRange sortedX 0 rowN labelId (.getColumns sortedX))
                                             chd {'threshold (Array/get sortedFtr (- labelId 1))
                                                  'featureId (- (nth rMap fId) 1)
                                                  'left      leftA
                                                  'right     rightA}]
                                            (setNds chd pAtoms)
                                            [[left [leftA] (labelAr left) false] [right [rightA] (labelAr right) false]])
                                       (do (setNds {:labelCounts (let [lAr (labelAr X)
                                                                       arN (count lAr)
                                                                       fTp (if classification (frequencies lAr) [[(average lAr) 1]])]
                                                                      (mapv (fn [[cId freq]] [cId (double (/ freq arN))]) fTp))
                                                    'type        'leaf}
                                                   pAtoms)
                                           [[X pAtoms Y true]])))) nodes))]
               (recur rMap nds (min newImpurity impurity) (if (< newImpurity impurity) 0 (+ 1 bedRes)) featN
                      (if (< newImpurity bestImpurity) 0 (+ 1 failsN)) (min newImpurity bestImpurity) (+ 1 cycle))))))

(defn findLeaf [tree v]
      (if (= (tree 'type) 'leaf)
        tree (let [{featureIdx 'featureId,
                    threshold   'threshold} tree
                   featureValue (v featureIdx)
                   child (if (<= featureValue threshold) 'left 'right)]
                  (findLeaf (deref (tree child)) v))))

(defn predict [tree v] (first (apply max-key second (let [leaf (findLeaf tree v)] (leaf :labelCounts)))))

(def time1 (/ (System/currentTimeMillis) 1000))

(defn classifAccuracy [exp real]
      (double (/ (apply + (map (fn [a b] (if (= (int (Math/round (double a))) (int (Math/round (double b)))) 1 0)) exp real)) (count real))))

(defn regrErr [exp real] (* 100 (/ (apply + (map (fn [a b] (Math/abs (- a b))) exp real)) (Math/abs (apply + real)))))

(defn runDS [dStream dt] (double-array (mapv (fn [v] (predict dStream v)) dt)))

(defn theSame [vs] (let [v (first vs)] (nil? (some (fn [v1] (not= v v1)) vs))))

(defn dStream []
      (let [fullData (.transpose (DoubleMatrix. (into-array (map double-array (mapv cons trainY trainX)))))
            obsN (.getColumns fullData)
            trDt (.toArray2 fullData)
            fIds (remove (fn [id] (theSame (nth trDt id))) (range 1 (.getRows fullData)))
            _ (println (count fIds) "features are selected from" (- (.getRows fullData) 1))
            rMap (int-array (vec (cons 0 fIds)))
            Xf fullData]
           (let [X (.getRows Xf rMap)
                 dStreamM (trainDStream X rMap)
                 trainRes (runDS dStreamM trainX)
                 expTestR (runDS dStreamM testDt)
                 testAcc ((if classification classifAccuracy regrErr) expTestR testRes)
                 trainAcc ((if classification classifAccuracy regrErr) trainRes trainY)]
                (println)
                (println "Test" (if classification "accuracy" "error") "=" testAcc)
                (println "Train" (if classification "accuracy" "error") "=" trainAcc)
                (println "Time" (int (- (/ (System/currentTimeMillis) 1000) time1)) "s")
                dStreamM)))

(defn csvReader [name] (CSVReader. (FileReader. name)))
(defn readCSV [reader] (do (.readNext reader)
                           (loop [vec []] (let [nextLine (.readNext reader)]
                                               (if (nil? nextLine) vec (recur (conj vec (mapv #(Double/parseDouble %) nextLine))))))))
(defn readData [fileName] (readCSV (csvReader (str baseDir fileName))))
(defn readAnswers [fileName] (mapv first (readData fileName)))

(defn -main [& args]
      (if (not= 7 (count args))
        (do (println "Wrong number of arguments.")
            (println "Usage: java -jar decision-stream.jar base-directory train-data train-answers test-data test-answers classification/regression significance-threshold")
            (println "Example: java -jar decision-stream.jar data/ailerons/ train_data.csv train_answ.csv test_data.csv test_answ.csv regression 0.02"))
        (binding [baseDir (nth args 0)]
                 (binding [trainX (readData (nth args 1))
                           trainY (readAnswers (nth args 2))
                           testDt (readData (nth args 3))
                           testRes (readAnswers (nth args 4))
                           classification (= "classification" (nth args 5))
                           pLevel (fn [] (Double/parseDouble (nth args 6)))
                           mgLevel (fn [] (Double/parseDouble (nth args 6)))]
                          (dStream)))))