import { h, VNode } from 'snabbdom';
import {
  spinner,
  bind,
  userName,
  dataIcon,
  player as renderPlayer,
  numberRow,
  matchScoreDisplay,
  multiMatchByeScore,
} from './util';
import { PairingExt, Outcome } from '../interfaces';
import { isOutcome } from '../util';
import SwissCtrl from '../ctrl';

interface MultiMatchOutcome {
  t: 'o';
  outcome: Outcome;
  round: number;
}

interface MultiMatchPairing extends PairingExt {
  t: 'p';
  round: number;
  ismm: boolean;
  mmGameNb: number;
  mmGameRes?: string;
  isFinalGame: boolean;
}

const isMultiMatchOutcome = (p: MultiMatchPairing | MultiMatchOutcome): p is MultiMatchOutcome => p.t === 'o';

const multiMatchGames = (sheet: (PairingExt | Outcome)[]): (MultiMatchPairing | MultiMatchOutcome)[] => {
  const newSheet: (MultiMatchPairing | MultiMatchOutcome)[] = [];
  let round = 1;
  sheet.forEach(v => {
    let gameNb = 1;
    if (isOutcome(v)) {
      newSheet.push({ t: 'o', round, outcome: v });
    } else if ((v.x || v.px) && v.mmids) {
      newSheet.push({
        t: 'p',
        round: round,
        ismm: true,
        mmGameRes: v.mr ? v.mr[0] : undefined,
        mmGameNb: 1,
        isFinalGame: false,
        ...v,
      });
      v.mmids.forEach(gid => {
        gameNb += 1;
        newSheet.push({
          t: 'p',
          round: round,
          ismm: true,
          mmGameRes: v.mr ? v.mr![gameNb - 1] : undefined,
          mmGameNb: gameNb,
          isFinalGame: gid == v.mmids![v.mmids!.length - 1],
          ...v,
          g: gid,
        });
      });
    } else {
      newSheet.push({ t: 'p', round: round, ismm: false, isFinalGame: true, mmGameNb: 1, ...v });
    }
    round += 1;
  });
  return newSheet;
};

export default function (ctrl: SwissCtrl): VNode | undefined {
  if (!ctrl.playerInfoId) return;
  const isMatchScore = ctrl.data.isMatchScore;
  const isMultiMatch = ctrl.data.nbGamesPerRound > 1;
  const data = ctrl.data.playerInfo;
  const noarg = ctrl.trans.noarg;
  const tag = 'div.swiss__player-info.swiss__table';
  if (data?.user.id !== ctrl.playerInfoId) return h(tag, [h('div.stats', [h('h2', ctrl.playerInfoId), spinner()])]);
  const games = isMultiMatch
    ? data.sheet.reduce((r, p) => r + ((p as any).mr || []).length, 0)
    : data.sheet.filter((p: any) => p.g).length;
  const wins = isMultiMatch
    ? data.sheet.reduce((r, p) => r + ((p as any).mr || []).filter((a: any) => a == 'win').length, 0)
    : data.sheet.filter((p: any) => p.w).length;
  const pairings = data.sheet.filter((p: any) => p.g).length;
  const avgOp: number | undefined = pairings
    ? Math.round(data.sheet.reduce((r, p) => r + ((p as any).rating || 1), 0) / pairings)
    : undefined;
  return h(
    tag,
    {
      hook: {
        insert: setup,
        postpatch(_, vnode) {
          setup(vnode);
        },
      },
    },
    [
      h('a.close', {
        attrs: dataIcon('L'),
        hook: bind('click', () => ctrl.showPlayerInfo(data), ctrl.redraw),
      }),
      h('div.stats', [
        h('h2', [h('span.rank', data.rank + '. '), renderPlayer(data, true, !ctrl.data.isMedley)]),
        h('table', [
          numberRow('Points', data.points, 'raw'),
          numberRow('Tiebreak' + (data.tieBreak2 ? ' [BH]' : ' [SB]'), data.tieBreak, 'raw'),
          data.tieBreak2 ? numberRow('Tiebreak [SB]', data.tieBreak2, 'raw') : null,
          ...(games
            ? [
                data.performance && !ctrl.data.isMedley
                  ? numberRow(noarg('performance'), data.performance + (games < 3 ? '?' : ''), 'raw')
                  : null,
                numberRow(noarg('winRate'), [wins, games], 'percent'),
                !ctrl.data.isMedley ? numberRow(noarg('averageOpponent'), avgOp, 'raw') : null,
              ]
            : []),
        ]),
      ]),
      h('div', [
        h(
          'table.pairings.sublist',
          {
            hook: bind('click', e => {
              const href = ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute('data-href');
              if (href) window.open(href, '_blank', 'noopener');
            }),
          },
          multiMatchGames(data.sheet).map(p => {
            const round = ctrl.data.round - p.round + 1;
            if (isMultiMatchOutcome(p)) {
              return h(
                'tr.' + p.outcome,
                {
                  key: round,
                },
                [
                  h('th', '' + round),
                  h('td.outcome', { attrs: { colspan: 3 } }, p.outcome),
                  h(
                    'td',
                    p.outcome == 'absent'
                      ? '-'
                      : p.outcome == 'bye'
                      ? isMatchScore
                        ? matchScoreDisplay(multiMatchByeScore(ctrl))
                        : '1'
                      : '½'
                  ),
                ]
              );
            }
            const res = result(p);
            return h(
              'tr.glpt.' + (p.o ? 'ongoing' : p.w === true ? 'win' : p.w === false ? 'loss' : 'draw'),
              {
                key: round,
                attrs: { 'data-href': '/' + p.g + (p.c ? '' : '/p2') },
                hook: {
                  destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement),
                },
              },
              [
                h('th', p.ismm ? '' + round + '.' + p.mmGameNb : '' + round),
                ctrl.data.isMedley && p.vi ? h('td', { attrs: { 'data-icon': p.vi } }, '') : null,
                h('td', userName(p.user)),
                h('td', ctrl.data.isMedley ? '' : '' + p.rating),
                h('td.is.playerIndex-icon.' + (p.c ? ctrl.data.p1Color : ctrl.data.p2Color)),
                h('td', gameResult(p) + (p.ismm && p.isFinalGame ? res : '')),
              ]
            );
          })
        ),
      ]),
    ]
  );
}

function gameResult(p: MultiMatchPairing): string {
  if (p.ismm) {
    switch (p.mmGameRes) {
      case 'win':
        return '(1) ';
      case 'loss':
        return '(0) ';
      case 'draw':
        return '(½) ';
      default:
        return '(*) ';
    }
  } else {
    return result(p);
  }
}

function result(p: MultiMatchPairing): string {
  if (p.ms) {
    return matchScoreDisplay(p.mp);
  }
  switch (p.w) {
    case true:
      return '1';
    case false:
      return '0';
    default:
      return p.o ? '*' : '½';
  }
}

function setup(vnode: VNode) {
  const el = vnode.elm as HTMLElement,
    p = playstrategy.powertip;
  p.manualUserIn(el);
  p.manualGameIn(el);
}
