import { Ctrl } from '../interfaces';

export default function status(ctrl: Ctrl): string {
  const noarg = ctrl.trans.noarg,
    d = ctrl.data;
  switch (d.game.status.name) {
    case 'started':
      return noarg('playingRightNow');
    case 'aborted':
      return noarg('gameAborted');
    case 'mate':
      switch (d.game.variant.lib) {
        case 0:
          return noarg('checkmate');
        case 1:
          return '';
        case 2:
          return noarg('checkmate');
      }
      return '';
    case 'resign':
      return ctrl.trans('sgPlayerResigned', d.game.loserPlayer);
    case 'stalemate':
      return noarg('stalemate');
    case 'timeout':
      return d.game.winner ? ctrl.trans('sgPlayerLeftTheGame', d.game.loserPlayer) : noarg('draw');
    case 'draw':
      return noarg('draw');
    case 'outoftime':
      //return `${d.game.turns % 2 === 0 ? noarg('whiteTimeOut') : noarg('blackTimeOut')}${
      //  d.game.winner ? '' : ` • ${noarg('draw')}`
      //}`;
      return d.game.winner ? ctrl.trans('sgPlayerTimeOut', d.game.loserPlayer) : `${ctrl.trans('sgPlayerTimeOut', '')} • ${noarg('draw')}`;
    case 'noStart':
      return d.game.loserPlayer + " didn't move";
    case 'cheat':
      return noarg('cheatDetected');
    case 'perpetualCheck':
      return noarg('perpetualCheck');
    case 'variantEnd':
      switch (d.game.variant.key) {
        case 'kingOfTheHill':
          return noarg('kingInTheCenter');
        case 'threeCheck':
          return noarg('threeChecks');
        case 'linesOfAction':
          return noarg('checkersConnected');
        case 'breakthrough':
          return noarg('promotion');
      }
      return noarg('variantEnding');
    case 'unknownFinish':
      return 'Finished';
    default:
      return d.game.status.name;
  }
}
