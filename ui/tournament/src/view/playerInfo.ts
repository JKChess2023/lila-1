import { h, VNode } from 'snabbdom';
import { spinner, bind, numberRow, playerName, dataIcon, player as renderPlayer } from './util';
import { teamName } from './battle';
import * as status from 'game/status';
import TournamentController from '../ctrl';

function result(win, stat): string {
  switch (win) {
    case true:
      return '1';
    case false:
      return '0';
    default:
      return stat >= status.ids.mate ? '½' : '*';
  }
}

function playerTitle(player) {
  return h('h2', [h('span.rank', player.rank + '. '), renderPlayer(player, true, false, false)]);
}

function setup(vnode: VNode) {
  const el = vnode.elm as HTMLElement,
    p = playstrategy.powertip;
  p.manualUserIn(el);
  p.manualGameIn(el);
}

export default function (ctrl: TournamentController): VNode {
  const data = ctrl.playerInfo.data;
  const noarg = ctrl.trans.noarg;
  const tag = 'div.tour__player-info.tour__actor-info';
  if (!data || data.player.id !== ctrl.playerInfo.id)
    return h(tag, [h('div.stats', [playerTitle(ctrl.playerInfo.player), spinner()])]);
  const nb = data.player.nb,
    pairingsLen = data.pairings.length,
    avgOp = pairingsLen
      ? Math.round(
          data.pairings.reduce(function (a, b) {
            return a + b.op.rating;
          }, 0) / pairingsLen
        )
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
        hook: bind('click', () => ctrl.showPlayerInfo(data.player), ctrl.redraw),
      }),
      h('div.stats', [
        playerTitle(data.player),
        data.player.team
          ? h(
              'team',
              {
                hook: bind('click', () => ctrl.showTeamInfo(data.player.team), ctrl.redraw),
              },
              [teamName(ctrl.data.teamBattle!, data.player.team)]
            )
          : null,
        h('table', [
          data.player.performance && !ctrl.data.medley
            ? numberRow(noarg('performance'), data.player.performance + (nb.game < 3 ? '?' : ''), 'raw')
            : null,
          numberRow(noarg('gamesPlayed'), nb.game),
          ...(nb.game
            ? [
                numberRow(noarg('winRate'), [nb.win, nb.game], 'percent'),
                numberRow(noarg('berserkRate'), [nb.berserk, nb.game], 'percent'),
                numberRow(noarg('averageOpponent'), avgOp, 'raw'),
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
          data.pairings.map(function (p, i) {
            const res = result(p.win, p.status);
            return h(
              'tr.glpt.' + (res === '1' ? ' win' : res === '0' ? ' loss' : ''),
              {
                key: p.id,
                attrs: { 'data-href': '/' + p.id + '/' + p.playerIndex },
                hook: {
                  destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement),
                },
              },
              [
                h('th', '' + (Math.max(nb.game, pairingsLen) - i)),
                ctrl.data.medley ? h('td', { attrs: { 'data-icon': p.variantIcon } }, '') : null,
                h('td', playerName(p.op)),
                h('td', ctrl.data.medley ? '' : p.op.rating),
                h('td.is.playerIndex-icon.' + p.playerColor),
                h('td', res),
              ]
            );
          })
        ),
      ]),
    ]
  );
}
