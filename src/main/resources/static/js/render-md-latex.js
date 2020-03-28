function renderMdLatexHtmlDecode(input){
    let e = document.createElement('textarea');
    e.innerHTML = input;
    return e.childNodes.length === 0 ? "" : e.childNodes[0].nodeValue;
}
// markdown + latex ($$ ~ $$) -> HTML
function renderMdLatex(text) {
    let md = window.markdownit().render(text), str = "", isLatex = false;
    md.split("$$").forEach(lat => {
        str += !isLatex ? lat : katex.renderToString(renderMdLatexHtmlDecode(lat), {
            fleqn: true,
            throwOnError: false,
        });
        isLatex = !isLatex;
    });
    return str;
}