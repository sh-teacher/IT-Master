/**
 * 구글 시트에 있는 학생 계정 명단을 studentuser.html에서 조회할 수 있도록 하는 웹 앱 스크립트.
 *
 * 보안을 위해 전체 명단을 절대 한 번에 반환하지 않고,
 *  - ?action=schools  요청에는 "학교명 목록"만 (개인정보 없음)
 *  - ?school=&cls=&name=&code= 요청에는 접근 코드가 맞을 때만
 *    정확히 일치하는 "그 한 명"의 정보만 돌려준다.
 * 즉 이 URL을 알아내도 접근 코드 + 학교/반/이름을 정확히 아는 사람 1명씩만
 * 조회 가능하고, 전체 계정을 한 번에 덤프할 수는 없다.
 *
 * 접근 코드 값은 이 파일(공개 저장소에 올라감)에 직접 적지 않고
 * Apps Script 프로젝트의 "스크립트 속성(Script Properties)"에 저장한다.
 * 설정 방법: Apps Script 편집기 좌측 톱니바퀴(프로젝트 설정) →
 * 스크립트 속성 → 속성 추가 → 이름: ACCESS_CODE, 값: (원하는 코드) → 저장.
 *
 * 시트 헤더는 원본 CSV와 동일해야 합니다:
 * 학교명, 이름, 학년-반, 기수, 계정, 비밀번호, 구독기간
 */
function doGet(e) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName('user') || ss.getSheets()[0];

  var values = sheet.getDataRange().getDisplayValues();
  var headers = values.shift().map(function (h) { return h.trim(); });

  var col = {};
  headers.forEach(function (h, i) { col[h] = i; });

  var required = ['학교명', '이름', '학년-반', '계정', '비밀번호', '구독기간'];
  var missing = required.filter(function (h) { return !(h in col); });
  if (missing.length) {
    return json({ error: '시트에 필요한 열이 없습니다: ' + missing.join(', ') });
  }

  var rows = values
    .filter(function (row) { return row[col['학교명']] && row[col['이름']]; })
    .map(function (row) {
      return {
        school: String(row[col['학교명']]).trim(),
        name: String(row[col['이름']]).trim(),
        cls: String(row[col['학년-반']]).trim(),
        account: String(row[col['계정']]).trim(),
        pw: String(row[col['비밀번호']]).trim(),
        period: String(row[col['구독기간']]).trim()
      };
    });

  var action = String(e.parameter.action || '').trim();

  // 학교 목록만 필요할 때: 개인정보(계정/비밀번호) 없이 학교명만 반환
  if (action === 'schools') {
    var schools = uniqueSorted(rows.map(function (r) { return r.school; }));
    return json({ schools: schools });
  }

  // 접근 코드 확인 (값은 코드에 없고 Script Properties에 저장되어 있음)
  var accessCode = getAccessCode();
  if (!accessCode) {
    return json({ error: 'setup_required' });
  }
  var code = String(e.parameter.code || '').trim();
  if (code !== accessCode) {
    return json({ error: 'invalid_code' });
  }

  // 조회: school + cls + name 세 가지가 모두 정확히 일치하는 "한 명"만 반환
  var school = String(e.parameter.school || '').trim();
  var cls = norm(e.parameter.cls);
  var name = norm(e.parameter.name);

  if (!school || !cls || !name) {
    return json({ error: 'missing_params' });
  }

  var match = rows.filter(function (r) {
    return r.school === school && norm(r.cls) === cls && norm(r.name) === name;
  })[0];

  if (!match) {
    return json({ error: 'not_found' });
  }

  return json({ result: match });
}

function norm(s) {
  return String(s || '').replace(/\s+/g, '').trim();
}

function getAccessCode() {
  return PropertiesService.getScriptProperties().getProperty('ACCESS_CODE');
}

function uniqueSorted(arr) {
  var seen = {};
  var out = [];
  arr.forEach(function (v) {
    if (!seen[v]) { seen[v] = true; out.push(v); }
  });
  return out.sort();
}

function json(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
