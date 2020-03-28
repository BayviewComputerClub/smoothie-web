function getVerdict(parsed) {
    if (parsed.submissionStatus === 'AWAITING_RUNNER') {
        return 'Waiting...';
    } else if (parsed.submissionStatus === 'JUDGING') {
        return 'Judging...'
    } else {
        return '<b>' + parsed.pointsAwarded + ' / ' + parsed.pointsMax + '</b>';
    }
}

function getSubLink(parsed) {
    if (parsed.permissionToView) {
        return '<a class="link" href="/submission/' + parsed.submissionId + '">Click to View</a>';
    } else {
        return "Can't View";
    }
}

function getStatus(parsed) {
    if (parsed.verdict === 'AC') {
        return '<span class="green">' + parsed.verdict + '</span>'
    } else if (parsed.verdict === 'WA') {
        return '<span class="red">' + parsed.verdict + '</span>'
    } else {
        return '<span class="grey">' + parsed.verdict + '</span>';
    }
}