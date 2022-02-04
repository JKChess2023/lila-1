import * as util from 'chessground/util';
import * as cg from 'chessground/types';
import RoundController from './ctrl';

export function flip(ctrl: RoundController, key: cg.Key) {
  console.log("flipping some pieces");
  const flipping: cg.Key[] = [];
  const  diff: cg.PiecesDiff = new Map();

  const orig : cg.Pos = util.key2pos(key);
  const directions: [number, number][] = [[1,1],[1,0],[1,-1],[0,1],[0,-1],[-1,1],[-1,0],[-1,-1]];
  var flip_list: cg.Key[] = [];
  for (let direction of directions) {
    flip_list = []
    for (var step = 1; step < ctrl.chessground.state.dimensions.width; step++){
        const pos : [number, number] = [orig[0] + step*direction[0], orig[1] + step*direction[1]];
        if (pos[0] < 1 || pos[0] > ctrl.chessground.state.dimensions.width || 
            pos[1] < 1 || pos[1] > ctrl.chessground.state.dimensions.height){
            break;
        }
        const k = util.pos2key(pos);
        const p = ctrl.chessground.state.pieces.get(k)
        if (p && p.playerIndex == ctrl.data.player.playerIndex){
            flip_list.forEach(x => flipping.push(x))
            break;
        }else if (p && p.playerIndex != ctrl.data.player.playerIndex){
            flip_list.push(util.pos2key(pos));
        }else{
            break;
        }
    }
  };

  const piece: cg.Piece = {role: 'p-piece', playerIndex: ctrl.data.player.playerIndex}
  flipping.forEach(x => diff.set(x, piece));
  ctrl.chessground.setPieces(diff);
}