app.keyboard$.subscribe(function(key) {
    if (key.mode === "global" && key.type === "j") {
        // scroll page down using javascript
        app.moveDown()
    } else if (key.mode === "global" && key.type === "k") {
        app.moveUp()
    } else if (key.mode === "global" && key.type === "h") {
        moveLeft()
    } else if (key.mode === "global" && key.type === "l") {
        moveRight()
    }
    key.claim()
  })
