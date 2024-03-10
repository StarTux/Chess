# Chess

Chess engine for Cavetale.

```
8     ♜ ♚   ♘   ♜ 
7     ♟ ♗   ♟     
6       ♟ ♟       
5 ♟           ♗ ♟ 
4       ♙ ♙       
3                 
2 ♙ ♙ ♙     ♙ ♙ ♙ 
1 ♖ ♘     ♔     ♖ 
  a b c d e f g h 
```

## Console Game

Running the jar will start a simple console chess game.

The following command line flags are available:

- `-f` or `--fen` Load the following FEN string
- `-w` or `--whitecpu` White is controlled by the CPU
- `-b` or `--blackcpu` Black is controlled by the CPU

The following console commands are available:

- `fen` Print current FEN string
- `loadfen` Read FEN until EOF
- `pgn` Print current PGN file
- `loadpgn` Read PGN file until EOF
- `board` Print the board
- `moves` Print all legal moves
- `random` Make random legal move
- `<move>` Make move
