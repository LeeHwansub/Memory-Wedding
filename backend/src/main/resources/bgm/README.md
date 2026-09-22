# BGM (프로젝트 공용)

하이라이트 영상 · 공개 청첩장에서 쓰는 **사용자 업로드 BGM**은 ObjectStorage에 저장됩니다.

- API: `POST /api/projects/{id}/bgm`
- 키 패턴: `{slug}/bgm/{uuid}_{filename}`
- 허용: MP3 / M4A / AAC / WAV (15MB 이하)

폴백용 서버 기본 음원(선택):

1. 라이선스 확인된 MP3를 이 폴더에 `default.mp3` 이름으로 둡니다.
2. 사용자 BGM이 없고 하이라이트에서 배경음악을 켠 경우에만 사용됩니다.
