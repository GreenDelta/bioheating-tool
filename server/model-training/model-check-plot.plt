set terminal pngcairo enhanced font "Rec Mono Casual,10"
set output 'data/model-check-plot.png'

set title "Expected vs. Predicted Heat Demand"
set xlabel "Expected [kWh]"
set ylabel "Predicted [kWh]"
set grid

set key outside below center horizontal

plot \
  'data/self-check.txt' using 1:2 \
    with points pointtype 7 pointsize 0.2 linecolor rgb "blue" \
    title "Training data", \
  'data/validation-check.txt' using 1:2 \
    with points pointtype 7 pointsize 1 linecolor rgb "red" \
    title "Validation data"

unset output
