(ns checkures.utils)

(def init-pos
  [[:white :white :white :white]
   [:white :white :white :white]
   [:white :white :white :white]
   [:none :none :none :none]
   [:none :none :none :none]
   [:red :red :red :red]
   [:red :red :red :red]
   [:red :red :red :red]])

(def edges
  #{#{[0 0] [0 1]}
    #{[0 0] [1 1]}
    #{[1 0] [1 1]}
    #{[1 0] [2 1]}
    #{[2 0] [2 1]}
    #{[2 0] [3 1]}
    #{[3 0] [3 1]}

    #{[0 1] [0 2]}
    #{[1 1] [0 2]}
    #{[1 1] [1 2]}
    #{[2 1] [1 2]}
    #{[2 1] [2 2]}
    #{[3 1] [2 2]}
    #{[3 1] [3 2]}

    #{[0 2] [0 3]}
    #{[0 2] [1 3]}
    #{[1 2] [1 3]}
    #{[1 2] [2 3]}
    #{[2 2] [2 3]}
    #{[2 2] [3 3]}
    #{[3 2] [3 3]}

    #{[0 3] [0 4]}
    #{[1 3] [0 4]}
    #{[1 3] [1 4]}
    #{[2 3] [1 4]}
    #{[2 3] [2 4]}
    #{[3 3] [2 4]}
    #{[3 3] [3 4]}

    #{[0 4] [0 5]}
    #{[0 4] [1 5]}
    #{[1 4] [1 5]}
    #{[1 4] [2 5]}
    #{[2 4] [2 5]}
    #{[2 4] [3 5]}
    #{[3 4] [3 5]}

    #{[0 5] [0 6]}
    #{[1 5] [0 6]}
    #{[1 5] [1 6]}
    #{[2 5] [1 6]}
    #{[2 5] [2 6]}
    #{[3 5] [2 6]}
    #{[3 5] [3 6]}

    #{[0 6] [0 7]}
    #{[0 6] [1 7]}
    #{[1 6] [1 7]}
    #{[1 6] [2 7]}
    #{[2 6] [2 7]}
    #{[2 6] [3 7]}
    #{[3 6] [3 7]}})

(defn get-2d
  [board col row]
  (get (get board row) col))

(defn set-2d
  [board col row v]
  (let [old-row (get board row)]
    (assoc board row (assoc old-row col v))))

(defn get-intermediate
  [[from-col from-row] dir dist-x]
  (cond
    (and (= dir :up) (> dist-x 0) (odd? from-row))
    [(dec from-col) (dec from-row)]
    (and (= dir :up) (< dist-x 0) (odd? from-row))
    [from-col (dec from-row)]
    (and (= dir :up) (> dist-x 0) (even? from-row))
    [from-col (dec from-row)]
    (and (= dir :up) (< dist-x 0) (even? from-row))
    [(inc from-col) (dec from-row)]
    (and (= dir :down) (> dist-x 0) (odd? from-row))
    [(dec from-col) (inc from-row)]
    (and (= dir :down) (< dist-x 0) (odd? from-row))
    [from-col (inc from-row)]
    (and (= dir :down) (> dist-x 0) (even? from-row))
    [from-col (inc from-row)]
    (and (= dir :down) (< dist-x 0) (even? from-row))
    [(inc from-col) (inc from-row)]
    :else nil))

(defn conn?
  "returns true if from is connected to to"
  [adj from to]
  (contains? adj #{from to}))

(defn get-dir
  ; TODO refactor to only take col?
  "returns the direction of a move from arg1 to arg2"
  [[_ y1] [_ y2]]
  (if (< y1 y2)
    :down
    :up))

(defn get-valid-dirs
  [piece]
  (case piece
    :red #{:up}
    :white #{:down}
    #{:up :down}))

(defn move
  [board [from-col from-row] [to-col to-row]]
  (let [piece (get-2d board from-col from-row)
        dir (get-dir [from-col from-row] [to-col to-row])
        dist-x (- from-col to-col)
        dist-y (- from-row to-row)
        intermediate (get-intermediate [from-col from-row] dir dist-x)]
    (if (or (= dist-y 1) (= dist-y -1))
      (-> board
          (set-2d from-col from-row :none)
          (set-2d to-col to-row piece))
      (-> board
          (set-2d from-col from-row :none)
          (set-2d (intermediate 0) (intermediate 1) :none)
          (set-2d to-col to-row piece)))))

(defn make-moves
  [board moves]
  (if (= (count moves) 2)
    (move board (moves 0) (moves 1))
    (let [zipped (map (fn [a b] [a b]) (pop moves) (rest moves))]
      (loop [b board
             m zipped]
        (if m
          (recur (move b ((first m) 0) ((first m) 1)) (next m))
          b)))))

(defn king-me
  [board]
  (-> board
      (assoc 0 (into [] (map #(if (= % :red) :red-king %) (get board 0))))
      (assoc 7 (into [] (map #(if (= % :white) :white-king %) (get board 7))))))

(defn valid-move?
  [board [from-col from-row] [to-col to-row]]
  (let [from-piece (get-2d board from-col from-row)
        to-piece (get-2d board to-col to-row)
        dir (get-dir [from-col from-row] [to-col to-row])
        valid-dirs (get-valid-dirs from-piece)
        dist-y (- from-row to-row)]
    (and
     (or (= dist-y 1) (= dist-y -1))
     (not= from-piece :none)
     (= to-piece :none)
     (conn? edges [from-col from-row] [to-col to-row]) ; TODO: keep as constant?
     (contains? valid-dirs dir))))

(defn valid-jump?
  [board [from-col from-row] [to-col to-row] og-piece]
  (let [opp-color (if (or (= :red og-piece) (= :red-king og-piece))
                    #{:white :white-king}
                    #{:red :red-king})
        to-piece (get-2d board to-col to-row)
        dir (get-dir [from-col from-row] [to-col to-row])
        valid-dirs (get-valid-dirs og-piece)
        dist-x (- from-col to-col)
        dist-y (- from-row to-row)
        intermediate (get-intermediate [from-col from-row] dir dist-x)
        int-piece (if intermediate (get-2d board (intermediate 0) (intermediate 1)) :none)]
    (and
     (or (= dist-y 2) (= dist-y -2))
     (= to-piece :none)
     (contains? opp-color int-piece)
     (contains? valid-dirs dir)
     (conn? edges [from-col from-row] intermediate)
     (conn? edges intermediate [to-col to-row]))))

(defn valid-turn?
  [board moves player]
  (when (> (count moves) 1)
    (let [[col1 row1] (first moves)
          [col2 row2] (fnext moves)
          dist-y (- row1 row2)
          first-piece (get-2d board col1 row1)
          valid-colors (if (= player :red) #{:red :red-king} #{:white :white-king})]
      (when (contains? valid-colors first-piece)
        (if (and (= (count moves) 2) (or (= dist-y 1) (= dist-y -1)))
          (valid-move? board [col1 row1] [col2 row2])
          (every? true? (map #(valid-jump? board %1 %2 first-piece) (pop moves) (rest moves))))))))
