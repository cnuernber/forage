(ns utils.math
    (:require [clojure.math.numeric-tower :as nt]
              [clojure.math :as math]
              [clojure.string :as st]))

; [fastmath.core :as fm]
; (use-primitive-operators)
; (unuse-primitive-operators)


;; TODO Consider revising to use clojure.math in Clojure 1.11:
;; https://clojure.org/news/2022/03/22/clojure-1-11-0
;; https://clojure.github.io/clojure/clojure.math-api.html


(defn remove-decimal-pt
  "Given a number, returns a (base-10) string representation of the
  number, but with any decimal point removed.  Also works on existing
  string representations of numbers."
  [x]
  (apply str 
         (st/split (str x) #"\.")))
  

;; Make my code a little prettier, and allow passing as functions:
;; TODO: Replace java.lang.Math with clojure.math?
(def pi Math/PI)
(defn cos [theta] (Math/cos theta))
(defn sin [theta] (Math/sin theta))
(defn tan [theta] (Math/tan theta))

(defn cartesian-to-polar
  "Convert Cartesian coordinates from x, y to [radius, angle]."
  [[x y]]
  [(math/sqrt (+ (* x x) (* y y))), (math/atan2 y x)]) ; note args to atan must be backwards

(defn polar-to-cartesian
  "Convert polar coordinates from radius r and angle theta to a
  pair of points [x y]."
  [[r theta]]
  [(* r (cos theta)) (* r (sin theta))])

(comment
  ;; How to convert from and back to polar coordinates:
  (let [original-r 5
        original-theta 0.5 
        [x y] (polar-to-cartesian original-r original-theta)
        [new-r new-theta] (cartesian-to-polar x y)]
    [x y original-r new-r original-theta new-theta])
)

(defn ln [x] (Math/log x))
(defn log [base x] (/ (ln x) (ln base)))


(defn rotate
  "Given an angle theta and a pair of coordinates [x y], returns a
  new pair of coordinates that is the rotation of [x y] by theta."
  [theta [x y]]
  [(- (* x (cos theta))
      (* y (sin theta))) ,
   (+ (* y (cos theta))
      (* x (sin theta)))])


(defn distance-2D
  "Computes distance between two-dimensional points [x0 y0] and [x1 y1]
  using the Pythagorean theorem."
  [[x0 y0] [x1 y1]]
  (let [xdiff (- x0 x1)
        ydiff (- y0 y1)]
  (nt/sqrt (+ (* xdiff xdiff) (* ydiff ydiff)))))


;; Implements $x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}\;$  given $\;ax^2 + bx + c = 0$.
;; (If both results are routinely needed inside a tight loop, consider making 
;; a version of this function that returns both of them.)
(defn quadratic-formula
  "Returns the result of the quadratic formula applied to the coefficients in
  ax^2 + bx + c = 0.  plus-or-minus should be one of the two functions: + - ."
  [plus-or-minus a b c]
  (let [root-part (nt/sqrt (- (* b b) (* 4 a c)))
        negb (- b)
        a2 (* 2 a)]
    (/ (plus-or-minus negb root-part) a2)))


;; Based on https://en.wikipedia.org/wiki/Spiral#Two-dimensional
(defn archimedean-spiral-pt
  "Returns 2D coordinates of a point on an Archimedean spiral
  corresponding to input theta (which may be any positive real).
  Parameter a determines how widely separated the arms are."
  [a theta]
  (let [r (* a theta)]
    [(* r (cos theta)) (* r (sin theta))]))


;; If this needed to be more efficient, the maps could be combined
;; with comb or a transducer.
(defn archimedean-spiral
  "Returns an infinite sequence of 2D coordinates of points on an
  Archimedean spiral around the origin.  Parameter a determines how
  widely separated the arms are.  increment is the distance between
  input values in radians; it determines the smoothness of a plot.  If x
  and y are provided, they move the center of the spiral to [x y].  If
  angle is provided, the entire spiral is rotated by angle radians."
  ([a increment] (map (fn [x] (archimedean-spiral-pt a (* increment x)))
                      (range)))
  ([a increment x y] (map (fn [[x' y']] [(+ x' x) (+ y' y)])
                          (archimedean-spiral a increment)))
  ([a increment x y angle] (map (comp (fn [[x' y']] [(+ x' x) (+ y' y)]) ; replace with transducer?
                                      (partial rotate angle)) ; rotation is around (0,0), so apply before shift
                                (archimedean-spiral a increment))))

;; On the name of the parameter arm-dist, cf. 
;; https://physics.stackexchange.com/questions/83760/what-is-the-space-between-galactic-arms-called
;; I'm calling this "unit" because the first argument is in
;; units of distance between arms.  Dunno.
(defn unit-archimedean-spiral
  "Returns an infinite sequence of 2D coordinate pairs of points on an
  Archimedean spiral around the origin.  Parameter arm-dist is the
  distance between arms or loops along a straight line from the center
  of the spiral.  increment is the distance between input values in
  radians; it determines the smoothness of a plot.  If x and y are
  provided, they move the center of the spiral to [x y].  If angle is
  provided, the entire spiral is rotated by angle radians."
  ([arm-dist increment]
   (archimedean-spiral (/ arm-dist 2 pi) increment))
  ([arm-dist increment x y]
   (archimedean-spiral (/ arm-dist 2 pi) increment x y))
  ([arm-dist increment x y angle]
   (archimedean-spiral (/ arm-dist 2 pi) increment x y angle)))

;; From 
;; https://en.wikipedia.org/wiki/Archimedean_spiral#Arc_length_and_curvature
;; cf. https://mathworld.wolfram.com/ArchimedesSpiral.html
;; NOTE No need to add a rotation; this is independent of rotation.
(defn archimedean-arc-len
  "Returns the length of an Archimedean spiral with parameter a from the
  center to angle x."
  [a x]
  (let [rootincsq (nt/sqrt (inc (* x x)))]
    (* a 0.5
       (+ (* x rootincsq)
          (math/log (+ x rootincsq))))))

;; NOTE No need to add a rotation; this is independent of rotation.
(defn unit-archimedean-arc-len
  "Returns the length of an Archimedean spiral with parameter arm-dist, in
  units of 1/2pi, from the center to angle x."
  [arm-dist x]
  (archimedean-arc-len (/ arm-dist 2 pi) x))

;; FIXME Seems to work with some rotations, but not with others.
(defn archimedean-arc-len-to-xy
  "UNTRUSTWORTHY:
  Returns the arc length of an Archimedean spiral with parameter a from
  its center to the location where it hits point [x y].  If angle is
  present, it is the rotation of the spiral."
  ([a [x y]]
   (archimedean-arc-len-to-xy a [0 0]               [x y] 0))
  ([a [center-x center-y] [x y]]
   (archimedean-arc-len-to-xy a [center-x center-y] [x y] 0))
  ([a [center-x center-y] [x y] angle]
   (let [r (distance-2D [center-x center-y] [x y])
         _ (println r) ; DEBUG
         theta (/ r a)] ; see my ~/math/randomwalks/spiral.nt1
     (archimedean-arc-len a (- theta angle)))))

;; FIXME Seems to work with some rotations, but not with others.
(defn unit-archimedean-arc-len-to-xy
  "UNTRUSTWORTHY:
  Returns the arc length of an Archimedean spiral with parameter
  arm-dist (i.e. distance between \"arms\") from its center to the
  location where it hits point [x y].  If angle is present, it is the
  rotation of the spiral."
  ([arm-dist [x y]]
   (unit-archimedean-arc-len-to-xy arm-dist [0 0]               [x y] 0))
  ([arm-dist [center-x center-y] [x y]]
   (unit-archimedean-arc-len-to-xy arm-dist [center-x center-y] [x y] 0))
  ([arm-dist [center-x center-y] [x y] angle]
   (let [r (distance-2D [center-x center-y] [x y])
         _ (println r) ; DEBUG
         theta (/ (* 2 pi r) arm-dist)] ; a=arm-dist/2pi, so r/a = r2pi/arm-dist
     (unit-archimedean-arc-len arm-dist (- theta angle)))))

(comment
  ;; Some ofthese examples show the arc-len-to-xy functions working,
  ;; but others don't.
  (require '[forage.viz.hanami :as h])
  (require '[oz.core :as oz])
  (oz/start-server!)

  (defn spir [rot]
    (->> (archimedean-spiral 2 0.01 50 50 rot)
         (h/order-walk-with-labels "spiral")
         (take 2000)
         (h/vega-walk-plot 600 100 1.0)
         (oz/view!)))

  (defn arcl
    "rot: rotation of spiral; edge: x coord of spiral at y value
    of center point; npi: radians from start to [edge, center-y]."
    [rot edge npi]
    [(archimedean-arc-len-to-xy 2 [50 50] [edge 50] rot)
     (archimedean-arc-len 2 (* npi pi))])

  (spir 0)
  (arcl 0 75 4)
  (spir (* pi 1/2))
  (arcl 1/2 72 (- 4 1/2))
  (spir (* pi 1/3))
  (arcl 1/3 73.035 (- 4 1/3))
  (spir (* pi 1/6))
  (arcl 1/6 74.1 (- 4 1/6))
  (spir (* pi 5/6))
  (arcl 5/6 69.9 (- 4 5/6))

  (defn uspir [rot]
    (->>
      (unit-archimedean-spiral 25 0.01 80 80 rot) ; rotated 90 degrees
      (h/order-walk-with-labels "spiral")
      (take 2000)
      (h/vega-walk-plot 600 160 1.0)
      (oz/view!)))

  (defn uarcl
    "rot: rotation of spiral; edge: x coord of spiral at y value
    of center point; npi: radians from start to [edge, center-y]."
    [rot edge npi]
    [(unit-archimedean-arc-len-to-xy 25 [80 80] [edge 80] rot)
     (unit-archimedean-arc-len 25 (* npi pi))])

  (uspir 0)
  (uarcl 0 130 4)
  (uarcl 0 155 6)

  (uspir pi)
  (uarcl pi 117.5 2)
  (uarcl pi 142.5 4)

  (uspir (/ pi 2))
  (uarcl (/ pi 2) 123.8 3)
  (uarcl (/ pi 2) 148.8 5)

  (uspir (* pi 1/3))
  (uarcl 1/3 125.84 (- 4 1/3))
  (uspir (* pi 1/6))
  (uarcl 1/6 127.9 (- 4 1/6))
  (uspir (* pi 5/6))
  (uarcl 5/6 119.6 (- 4 5/6))


  ;; for both arcl and uarcl, the rule seems to be that rot
  ;; subtracts (* 2 rot) from the rotation. Which doesn't make sense.

  ;(require '[nextjournal.clerk :as clerk])
  ;(clerk/serve! {:browse? true :watch-paths ["src/clj"]})
  ;(clerk/vl plot)
)


(defn bool-to-bin
  "Returns 1 if x is truthy, 0 if it's falsey."
  [x]
  (if x 1 0))

(defn sign
  [x]
  (cond (pos? x) 1
        (neg? x) -1
        :else 0))

;; Note that Java's Double/isInfinite and Float/isInfinite don't distinguish 
;; between ##Inf and ##-Inf.
(defn pos-inf?
  "Returns true if and only if x is ##Inf."
  [x]
  (= x ##Inf))

;; Added to Clojure in 1.11
;; Just a wrapper for Double/isNaN
;(defn NaN?
;  "Returns true if and only if x is ##NaN."
;  [x]
;  (Double/isNaN x))

(defn slope-from-coords
  "Given a pair of points on a line, return its slope.  This is also the
  vector direction from the first point to the second.  If the line is
  vertical, returns ##Inf (infinity) to indicate that."
  [[x1 y1] [x2 y2]]
  (if (== x1 x2)
    ##Inf ; infinity is what division below would give for the vertical slope
    (/ (- y2 y1) (- x2 x1))))

;; y = mx + b  so  b = y - mx
(defn intercept-from-slope
  "Given a slope and a point on a line, return the line's y intercept."
  [slope [x y]]
  (- y (* slope x)))

(defn equalish?
  "True if numbers x and y are == or are within (* n-ulps ulp) of 
  each other, where ulp is the minimum of (Math/ulp x) and (Math/ulp y).
  A ulp is \"units in the last place\", i.e. the minimum possible difference
  between two floating point numbers, but the numeric value of a ulp differs
  depending on the number, even within the same numeric class such as double.
  We use the minimum since that's the least difference between one of the
  numbers and the next one up or down from  it.  (It seem as if multiplying a
  number that's one ulp off produces a number that is some power of 2 ulp's
  away from the correct value.) See java.lang.Math for more."
  [n-ulps x y]
  (or (== x y)
      (let [xd (double x) ; Math/ulp doesn't work on integers
            yd (double y)
            ulp (min (Math/ulp xd) (Math/ulp yd))]
        (<= (abs (- xd yd))
            (* n-ulps ulp)))))


(defn mean
  "Returns the mean value of all numbers in collection xs, or the
  first n values if n is provided.  If n greater than the length of xs,
  takes the mean of xs."
  ([xs]
   (let [n (count xs)]
     (/ (reduce + xs) n)))
  ([n xs] (mean (take n xs)))) ; don't divide by n explicitly: xs may be short

(defn count-decimal-digits
  "Given a number, returns the number of digits in the decimal
  representation of its integer part."
  [n]
  (count (str (nt/round n))))

;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; USE APACHE COMMONS PARETO DISTRIBUTION INSTEAD:

  ;; Pareto PDF: $\mathsf{P}(x) = \frac{\alpha x_m^{\alpha}}{x^{\alpha + 1}}$, again for $x \leq x_m$.
  ;; (Note that memoizing this makes it slower.  Rearranging to use expt only
  ;; once also makes it slower.)
  (defn pareto
    "Given a scale parameter x_m (min value, should be positive) and a shape parameter
    alpha (positive), returns the value of the Pareto density function at x
    (from https://en.wikipedia.org/wiki/Pareto_distribution).  Returns 0
    if x < minumum"
    [xm alpha x]
    (if (< x xm)
      0
      (/ (* alpha (nt/expt xm alpha))
         (nt/expt x (inc alpha)))))

  ; Assuming that $\mu > 1$, 
  ; $\int_r^{\infty} x^{-\mu} \; dl = \frac{r^{1-\mu}}{\mu-1} \,$.
  ; &nbsp; So to distribute step lengths $x$ as $x^{-\mu}$ with $r$ as 
  ; the minimum length,
  ; $\mathsf{P}(x) = x^{-\mu}\frac{\mu-1}{r^{1-\mu}} = x^{-\mu}r^{\mu-1}(\mu-1)$.
  ;; &nbsp; See steplengths.md for further details.  &nbsp; cf. Viswanathan et al., *Nature* 1999.
  ;; This can be viewed as a Pareto distribution, but parameterized differently.
  (defn powerlaw
    "Returns probability of x with normalized density x^mu, where r is
    x's minimum value.  Returns 0 if x < minumum."
    [r mu x]
    (if (< x r)
      0
      (let [mu- (dec mu)]
        (* (nt/expt x (- mu)) (nt/expt r mu-) mu-))))
)
