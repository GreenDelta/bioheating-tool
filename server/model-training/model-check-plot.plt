set terminal pngcairo enhanced font "Rec Mono Casual,10"

set grid
set key outside below center horizontal

# Heat demand (columns 1 and 2 of the check files)
set title "Expected vs. Predicted Heat Demand"
set xlabel "Expected [kWh]"
set ylabel "Predicted [kWh]"
set output 'data/model-check-heat-demand.png'
plot \
  'data/self-check.txt' using 1:2 \
    with points pointtype 7 pointsize 0.2 linecolor rgb "blue" \
    title "Training data", \
  'data/validation-check.txt' using 1:2 \
    with points pointtype 7 pointsize 1 linecolor rgb "red" \
    title "Validation data"

# Peak load (columns 3 and 4 of the check files)
set title "Expected vs. Predicted Peak Load"
set xlabel "Expected [kW]"
set ylabel "Predicted [kW]"
set output 'data/model-check-peak-load.png'
plot \
  'data/self-check.txt' using 3:4 \
    with points pointtype 7 pointsize 0.2 linecolor rgb "blue" \
    title "Training data", \
  'data/validation-check.txt' using 3:4 \
    with points pointtype 7 pointsize 1 linecolor rgb "red" \
    title "Validation data"

unset output
