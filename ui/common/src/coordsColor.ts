const changeColorHandle = (): void => {
  const dict: { [theme: string]: string } = {
    'chess-blue': '#DEE3E6 #788a94',
    'chess-blue2': '#97b2c7 #546f82',
    'chess-blue3': '#d9e0e6 #315991',
    'chess-canvas': '#d7daeb #547388',
    'chess-wood': '#d8a45b #9b4d0f',
    'chess-wood2': '#a38b5d #6c5017',
    'chess-wood3': '#d0ceca #755839',
    'chess-wood4': '#caaf7d #7b5330',
    'chess-maple': '#e8ceab #bc7944',
    'chess-maple2': '#E2C89F #996633',
    'chess-leather': '#d1d1c9 #c28e16',
    'chess-green': '#FFFFDD #6d8753',
    'chess-brown': '#F0D9B5 #946f51',
    'chess-pink': '#E8E9B7 #ED7272',
    'chess-marble': '#93ab91 #4f644e',
    'chess-blue-marble': '#EAE6DD #7C7F87',
    'chess-green-plastic': '#f2f9bb #59935d',
    'chess-grey': '#b8b8b8 #7d7d7d',
    'chess-metal': '#c9c9c9 #727272',
    'chess-olive': '#b8b19f #6d6655',
    'chess-newspaper': '#fff #8d8d8d',
    'chess-purple': '#9f90b0 #7d4a8d',
    'chess-purple-diag': '#E5DAF0 #957AB0',
    'chess-ic': '#ececec #c1c18e',
    'chess-horsey': '#F0D9B5 #946f51',
    'loa-blue': '#DEE3E6 #788a94',
    'loa-blue2': '#97b2c7 #546f82',
    'loa-blue3': '#d9e0e6 #315991',
    'loa-canvas': '#d7daeb #547388',
    'loa-wood': '#d8a45b #9b4d0f',
    'loa-wood2': '#a38b5d #6c5017',
    'loa-wood3': '#d0ceca #755839',
    'loa-wood4': '#caaf7d #7b5330',
    'loa-maple': '#e8ceab #bc7944',
    'loa-maple2': '#E2C89F #996633',
    'loa-leather': '#d1d1c9 #c28e16',
    'loa-green': '#FFFFDD #6d8753',
    'loa-brown': '#F0D9B5 #946f51',
    'loa-pink': '#E8E9B7 #ED7272',
    'loa-marble': '#93ab91 #4f644e',
    'loa-blue-marble': '#EAE6DD #7C7F87',
    'loa-green-plastic': '#f2f9bb #59935d',
    'loa-grey': '#b8b8b8 #7d7d7d',
    'loa-metal': '#c9c9c9 #727272',
    'loa-olive': '#b8b19f #6d6655',
    'loa-newspaper': '#fff #8d8d8d',
    'loa-purple': '#9f90b0 #7d4a8d',
    'loa-purple-diag': '#E5DAF0 #957AB0',
    'loa-ic': '#ececec #c1c18e',
    'loa-horsey': '#F0D9B5 #946f51',
  };

  for (const theme of document.body.className.split(' ')) {
    if (theme in dict) {
      const style = document.documentElement.style,
        colors = dict[theme].split(' ');
      const gf = theme.split('-')[0];
      style.setProperty(`--cg-coord-color-${gf}-white`, colors[0]);
      style.setProperty(`--cg-coord-color-${gf}-black`, colors[1]);
      style.setProperty(`--cg-coord-shadow-${gf}`, 'none');
    }
  }
};

export default changeColorHandle;
