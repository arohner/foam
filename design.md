This document describes Foam's design, and potential ways to integrate it with Om.

# Motivation

Single Page Apps (SPA) are powerful and provide rich UI, but the total
network size to load an SPA is typically larger than a conventional
website. This larger network size increases page load times, which
harms the user experience.

# Goals

Our goal is to be able to render an Om app, server-side (generating an
HTML string), in Clojure (rather than ClojureScript). This should
result in a page render significantly sooner than serving app.js to a
browser and rendering browser-side.

Foam should compile all Om programs; all public Om functions are
defined, though not all Om functions require implementations. All
functions that make sense for use in server-side rendering are
implemented (IRender, cursors, app-state, local-state, etc). Functions
that don't make sense in server-side context are no-ops (WillMount,
DidMount, DidUnMount, etc). Implemented functions should behave 'the
same' as Om.

Rendered Foam components should generate the same react-ids as Om, to
allow browser-side diffing to work when browser-side rendering takes
over.

# Design

Foam shares code with Om whenever possible, to reduce effort, and to
reduce bugs.

Foam implements the minimum possible to make render-to-string work,
and still provide API compatibility.

Cursors, State and Transactions are largely copy & pasted from Om.

# New Protocols

Foam defines two new protocols to make up for the lack of React.js:
`ReactRender`, and `ReactDOMRender`. ReactDOMRender is the simpler of
the two. ReactDOMRender is used for actual DOM nodes, such as `div`
`h1` and text strings. It defines a function, render-to-string, which
returns an HTML string.

`ReactRender` behaves similar to React's `render` function. It forces
the construction of Om components, and returns a tree of 'DOM Node'
records that implement ReactDOMRender.

# om.dom

`om.dom` defines functions such as `h1`, `p`, which return JS objects
that implement React's virtual DOM protocol to participate in diffing
and rendering. `foam.dom` implements all of the same functions, with
the same signatures. foam.dom functions instead return defrecords,
that implement `ReactDOMRender`. Foam doesn't need to diff, so the
other parts of the React API are unnecessary.

foam.dom currently uses code from hiccup to render HTML.


# Constructing

The main difference between Foam and Om is in om/build. om/build
creates a JS object that holds references to the app-state, cursor and
holds the Om component. Om uses descriptors to specify implementations
of React protocols. Currently, Foam does not implement descriptors.

`foam/build` instead creates a defrecord OmComponent that holds the same
information (app-state, cursor, om component). OmComponent implements
ReactRender.

# Missing Features

- descriptors
- cursors other than MapCursor

# Suggestions For Merging With Om

- port Om to .cljc
- in om.dom, make the #clj branch of the dom element functions call `foam.dom/element`
- in om.core, make the #clj branch of `om.core/build` call `foam.core/build`