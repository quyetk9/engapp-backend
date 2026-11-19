'use strict';
// Noi dung bai hoc. Sua file nay roi khoi dong lai server la bai hoc duoc cap nhat.
//
// 5 dang bai tap:
//  card   - the tu vung (hoc tu moi)
//  match  - ghep tu voi hinh (emoji)
//  fill   - dien tu vao cho trong (1 hoac nhieu cho trong, danh dau bang ___)
//  listen - nghe va chon dap an dung
//  order  - sap xep cac tu theo dung thu tu

const card = (word, ipa, meaningVi, emoji) => ({ type: 'card', word, ipa, meaningVi, emoji });
const match = (...pairs) => ({ type: 'match', pairs: pairs.map(([word, emoji]) => ({ word, emoji })) });
const fill = (promptVi, template, answers, options) => ({ type: 'fill', promptVi, template, answers, options });
const listen = (promptVi, options, answerIndex) => ({ type: 'listen', promptVi, options, answerIndex });
const order = (promptVi, answer, extra = []) => ({ type: 'order', promptVi, answer, words: [...answer, ...extra] });

const LESSONS = [
  {
    id: 1,
    title: 'Cửa hàng và nhà hàng',
    exercises: [
      match(['milk', '🥛'], ['sugar', '🍬'], ['coffee', '☕']),
      card('Rice', 'raɪs', 'cơm’ hoặc ‘gạo', '🍚'),
      fill('Chúng tôi cần cơm.', 'We need ___.', ['rice'], ['soda', 'rice', 'curry', 'water']),
      fill('Có gạo ở siêu thị.', 'There is ___ in the ___.', ['rice', 'supermarket'], ['supermarket', 'sandwiches', 'rice', 'apple']),
      listen('Họ không có cơm.', ['They are on the right.', 'They do not have rice.'], 1),
      card('Oil', 'ɔɪl', 'dầu ăn', '🫒'),
      fill('Dầu ăn ở đâu?', '___ is the ___?', ['Where', 'oil'], ['oil', 'How', 'Where', 'milk']),
      card('Bread', 'brɛd', 'bánh mì', '🍞'),
      listen('Tôi muốn mua bánh mì.', ['I want to buy bread.', 'I want to buy milk.'], 0),
      order('cơm hoặc dầu ăn', ['Rice', 'or', 'oil'], ['Salt', 'and']),
      order('Chúng tôi cần bánh mì.', ['We', 'need', 'bread'], ['rice', 'is']),
    ],
  },
  {
    id: 2,
    title: 'Địa điểm trong thành phố',
    exercises: [
      card('Library', 'ˈlaɪbrɛri', 'thư viện', '📚'),
      card('Park', 'pɑːrk', 'công viên', '🌳'),
      match(['library', '📚'], ['park', '🌳'], ['hospital', '🏥']),
      fill('Tôi đang ở công viên.', 'I am in the ___.', ['park'], ['park', 'library', 'bank', 'hotel']),
      card('Hospital', 'ˈhɒspɪtl', 'bệnh viện', '🏥'),
      listen('Bệnh viện ở bên trái.', ['The hospital is on the left.', 'The hospital is on the right.'], 0),
      card('Bank', 'bæŋk', 'ngân hàng', '🏦'),
      fill('Ngân hàng ở đâu?', '___ is the ___?', ['Where', 'bank'], ['bank', 'Where', 'How', 'park']),
      order('Thư viện ở gần đây.', ['The', 'library', 'is', 'near'], ['park', 'bank']),
      match(['school', '🏫'], ['hotel', '🏨'], ['bank', '🏦']),
    ],
  },
  {
    id: 3,
    title: 'Gia đình',
    exercises: [
      card('Mother', 'ˈmʌðər', 'mẹ', '👩'),
      card('Father', 'ˈfɑːðər', 'bố', '👨'),
      match(['mother', '👩'], ['father', '👨'], ['baby', '👶']),
      fill('Đây là mẹ của tôi.', 'This is my ___.', ['mother'], ['mother', 'father', 'sister', 'brother']),
      card('Sister', 'ˈsɪstər', 'chị/em gái', '👧'),
      listen('Tôi có một chị gái.', ['I have a sister.', 'I have a brother.'], 0),
      card('Grandmother', 'ˈɡrænmʌðər', 'bà', '👵'),
      fill('Bà của tôi ở nhà.', 'My ___ is at home.', ['grandmother'], ['grandmother', 'school', 'bread', 'park']),
      order('Gia đình tôi có bốn người.', ['My', 'family', 'has', 'four', 'people'], ['two', 'is']),
      match(['sister', '👧'], ['grandmother', '👵'], ['father', '👨']),
    ],
  },
];

module.exports = { LESSONS };
