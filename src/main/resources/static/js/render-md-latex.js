// markdown + latex ($$ ~ $$) -> HTML
function renderMdLatex(text) {
    let md = window.markdownit().render(text), str = "", isLatex = false;
    md.split("$$").forEach(lat => {
        str += !isLatex ? lat : katex.renderToString(lat, {
            fleqn: true,
            throwOnError: false,
        });
        isLatex = !isLatex;
    });
    return str;
}