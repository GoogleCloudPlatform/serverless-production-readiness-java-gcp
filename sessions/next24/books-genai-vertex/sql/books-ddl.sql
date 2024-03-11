-- setup alloy db follow these steps on codelabs https://codelabs.developers.google.com/codelabs/alloydb-ai-embedding#4

-- 1. Authors Table
create TABLE authors (
    author_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    bio TEXT,
    embedding public.vector GENERATED ALWAYS AS (public.embedding('textembedding-gecko@003'::text, bio)) STORED
);

-- 2. Books Table
create TABLE books (
    book_id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author_id INT NOT NULL,
    publication_year date,
    CONSTRAINT fk_author
        FOREIGN KEY(author_id)
        REFERENCES Authors(author_id)
);
CREATE TYPE scope_type AS ENUM ('public', 'private');

ALTER TABLE books ADD COLUMN scope scope_type NOT NULL DEFAULT 'public';

--CREATE TABLE books (
--    book_id SERIAL PRIMARY KEY,
--    title VARCHAR(255) NOT NULL,
--    author_id INT NOT NULL,
--    publication_year date,
--    scope VARCHAR(7) CHECK (scope IN ('public', 'private')),
--    CONSTRAINT fk_author
--        FOREIGN KEY(author_id)
--        REFERENCES Authors(author_id)
--);

-- 3. Chapters Table
--create TABLE chapters (
--    chapter_id SERIAL PRIMARY KEY,
--    book_id INT NOT NULL,
--    title VARCHAR(255) NOT NULL,
--    number INT NOT NULL,
--    CONSTRAINT fk_book
--        FOREIGN KEY(book_id)
--        REFERENCES Books(book_id)
--        ON delete CASCADE
--);

--- ALTER TABLE cymbal_products ADD COLUMN embedding vector GENERATED ALWAYS AS (embedding('textembedding-gecko@001',product_description)) STORED;

-- 4. Pages Table
create TABLE public.pages (
    page_id SERIAL PRIMARY KEY,
    book_id INT NOT NULL,
    page_number INT NOT NULL,
    content TEXT,
    embedding public.vector GENERATED ALWAYS AS (public.embedding('textembedding-gecko@003'::text, content)) STORED,
    CONSTRAINT fk_pages
        FOREIGN KEY(book_id)
        REFERENCES Books(book_id)
);

-- ALTER TABLE public.pages DROP COLUMN chapter_id;
-- Drop TABLE public.pages;

-- 5. BookSummaries Table
create TABLE bookSummaries (
    summary_id SERIAL PRIMARY KEY,
    book_id INT UNIQUE NOT NULL,
    summary TEXT NOT NULL,
    embedding public.vector GENERATED ALWAYS AS (public.embedding('textembedding-gecko@003'::text, summary)) STORED,
    CONSTRAINT fk_book_summary
        FOREIGN KEY(book_id)
        REFERENCES Books(book_id)
);


-- ALTER TABLE authors ADD COLUMN embedding vector GENERATED ALWAYS AS (embedding('textembedding-gecko@001', bio)) STORED;

select
*
from
    books
     where title = 'The Complete Works of William Shakespeare';

--update books set title='The_Complete_Works_of_William_Shakespeare' where book_id=8;

select
*
from
    books;

select
*
from
    authors;

select * from books where title = 'The Complete Works of William Shakespeare';
select * from bookSummaries
where book_id=?;

select
*
from
    pages where book_id=22 order by page_number desc;
--delete from authors where name = 'Marcus Aurelius Antoninus';
--delete from pages where book_id=11;
--delete from books where book_id=11;
--delete from pages where book_id=11;
--delete from books where author_id= 12;
--update books set title='The Complete Works of William Shakespeare' where book_id=8;
--update authors set name='William Shakespeare' where author_id=1;




insert into authors (
    "bio",
    "name")
values (
    'William Shakespeare was an English playwright, poet and actor. He is widely regarded as the greatest writer in the English language and the world pre-eminent dramatist. He is often called England national poet and the "Bard of Avon". ',
    'William Shakespeare');

insert into authors (
    "bio",
    "name")
values ('famous author', 'Yanni Shakespeare');

-- update public.authors set bio = 'James Augustine Aloysius Joyce was an Irish novelist, poet, and literary critic. He contributed to the modernist avant-garde movement and is regarded as one of the most influential and important writers of the 20th century.'  where author_id = 2;

insert into authors (
    "bio",
    "name")
values (
    'Plato was an ancient Greek philosopher of the Classical period who is considered a top thinker in Philosophy. He is the namesake of Platonic love and the Platonic solids. He founded the Academy, a philosophical school in Athens where Plato taught the doctrines that would later become known as Platonism.',
    'Plato');

insert into authors (
    "bio",
    "name")
values (
   'James Augustine Aloysius Joyce was an Irish novelist, poet, and literary critic. He contributed to the modernist avant-garde movement and is regarded as one of the most influential and important writers of the 20th century.',
   'James Joyce');

insert into authors (
    "bio",
    "name")
values (
   'Marcus Aurelius Antoninus (26 April 121 – 17 March 180) was Roman emperor from 161 to 180 and a Stoic philosopher. He was a member of the Nerva–Antonine dynasty, the last of the rulers later known as the Five Good Emperors and the last emperor of the Pax Romana, an age of relative peace, calm, and stability for the Roman Empire lasting from 27 BC to 180 AD. He served as Roman consul in 140, 145, and 161.',
   'Marcus Aurelius Antoninus');

insert into authors (
    "bio",
    "name")
values (
   'Joseph Rudyard Kipling was an English novelist, short-story writer, poet, and journalist. He was born in British India, which inspired much of his work. Kipling''s works of fiction include the Jungle Book duology, Kim, the Just So Stories and many short stories, including "The Man Who Would Be King".',
   'Rudyard Kipling');

-- update books set publication_year='1922-01-01' where author_id=3;

insert into books (
    author_id,
    publication_year,
    title)
values (
    1,
    '1623-01-01',
    'The Complete Works of William Shakespeare - Shakespeare')
;

insert into books (
    author_id,
    publication_year,
    title)
values (
    2,
    '0375-01-01 BC',
    'The Republic - Plato')
;

insert into books (
    author_id,
    publication_year,
    title)
values (
    6,
    '0161-01-01',
    'Meditations - Marcus Aurelius')
;

insert into books (
    author_id,
    publication_year,
    title)
values (
    7,
    '1894-01-01',
    'The Jungle Book - Rudyard Kipling')
;

-- update books set publication_year='0375-01-01 BC' where author_id

insert into books (
    author_id,
    publication_year,
    title)
values (
    3,
    '1922-02-02',
    'Ulysses - James Joyce')
;

--8	The Complete Works of William Shakespeare - Shakespeare	1	Jan 1, 1623, 12:00:00 AM
--9	The Republic - Plato	2	Jan 1, 375, 12:00:00 AM
--10	Ulysses - James Joyce	3	Feb 2, 1922, 12:00:00 AM
-- prompt Fix the following sql insert statement escape ' in the summary string that is causing the insert statement to error:
-- prompt Give me a summary of The Jungle Book - Rudyard Kipling in less than 10 paragraphs with no special characters, line breaks, and headings.
select
*
from
    booksummaries;

-- https://www.toolsoverflow.com/string/paragraph-to-line use book id after books have been inserted..
INSERT INTO booksummaries (book_id, summary) VALUES (21, 'In Plato''s "The Republic," Socrates embarks on a philosophical exploration of justice, the ideal state, and human nature. The dialogue begins with a discussion on the nature of justice, challenging the conventional view that it is simply "doing good to friends and harm to enemies." Socrates argues that justice extends beyond personal relationships and is an essential virtue for both individuals and society. To understand justice, Socrates suggests creating an ideal state, a hypothetical society where justice can be observed in its pure form. This state is envisioned as a harmonious society consisting of three classes: the rulers, the auxiliaries, and the producers. The rulers, who are selected based on their wisdom and virtue, are responsible for governing and ensuring the well-being of the state. The auxiliaries, who possess courage and strength, serve as the military and police force, protecting the state from external and internal threats. The producers, who comprise the majority of the population, engage in labor and provide for the material needs of the state. In this ideal state, justice is achieved when each class performs its proper function. The rulers rule wisely, the auxiliaries protect courageously, and the producers work diligently. Individuals are assigned roles based on their natural abilities and inclinations, ensuring that the needs of society are met while also allowing each person to flourish. Socrates emphasizes the importance of education in shaping just individuals. He advocates for a rigorous curriculum that combines intellectual, physical, and moral training. Such an education would produce individuals who are virtuous, knowledgeable, and capable of making sound judgments. "The Republic" also explores the nature of human desires and the role they play in shaping behavior. Socrates argues that human beings are motivated by three main desires: the desire for pleasure, the desire for honor, and the desire for knowledge. While each of these desires can be beneficial in moderation, excessive pursuit of any one desire can lead to injustice and unhappiness. In the context of the ideal state, Socrates argues for the suppression of excessive desires, particularly the desire for pleasure. He believes that a life devoted to sensual gratification undermines the pursuit of virtue and prevents individuals from achieving true happiness. While "The Republic" presents a compelling vision of an ideal state, it also acknowledges the challenges of implementing such a society. Socrates recognizes that human nature is imperfect and that achieving perfect justice may be elusive. Nonetheless, he encourages striving for justice as the highest human good, believing that even partial success in this pursuit can lead to a more harmonious and fulfilling life for both individuals and society.');
INSERT INTO booksummaries (book_id, summary) VALUES (8, 'William Shakespeare''s literary canon encompasses a staggering array of plays, poems, and sonnets, each a masterpiece in its own right. His plays, spanning comedies, histories, and tragedies, present a profound exploration of human nature in all its complexities. From the witty banter of "A Midsummer Night''s Dream" to the tragic downfall of "Hamlet," Shakespeare''s plays delve into themes of love, ambition, jealousy, and betrayal. His characters are unforgettable, from the star-crossed lovers Romeo and Juliet to the power-hungry Macbeth. Shakespeare''s ability to portray the intricacies of the human condition through language and characterization is unmatched. In his sonnets, Shakespeare explores the depths of love, beauty, and time with exquisite lyrical precision. The "Sonnets" are a celebration of the transformative power of love, its joys and sorrows, its triumphs and despair. They offer a glimpse into Shakespeare''s soul, revealing his innermost thoughts and emotions. Shakespeare''s plays and poems were written in a time of great social and political change, and they often reflect the tumultuous events of his day. "King Lear," for example, explores the themes of madness, loyalty, and generational conflict amidst the chaos of medieval England. "The Tempest," set on a magical island, allegorically examines the struggle between the old world and the new, as well as the transformative power of forgiveness. Shakespeare''s mastery of language is evident in his use of imagery, wordplay, and rhythm. His works are filled with memorable lines, such as "To be or not to be" from "Hamlet" and "All the world''s a stage" from "As You Like It." His ability to weave words together creates a tapestry of sound and meaning that captivates audiences to this day. The Complete Works of William Shakespeare is a testament to the enduring power of his writing. It is a collection that has inspired countless adaptations, performances, and scholarly studies over the centuries. It is a body of work that has shaped the English language and continues to resonate with readers and audiences around the world. In conclusion, William Shakespeare''s Complete Works is a literary treasure that offers a profound exploration of the human experience. His plays, poems, and sonnets are masterpieces of language and characterization that have stood the test of time. They are a testament to Shakespeare''s genius and his ability to capture the essence of humanity in all its glory and complexity.');
INSERT INTO booksummaries (book_id, summary) VALUES (10, 'Ulysses, a modernist masterpiece by James Joyce, transports readers into the labyrinthine streets and introspective realm of Dublin on a single day in June 1904. Leopold Bloom, a Jewish advertising salesman, emerges as the unlikely protagonist. His mundane existence transforms into an epic journey through the city''s social, cultural, and psychological landscapes. Joyce weaves together Homer''s Odyssey with Bloom''s wanderings, mirroring ancient myths in the tapestry of modern life. Bloom encounters a colorful cast of characters, including Stephen Dedalus, a young poet and Joyce''s alter ego. Dedalus embodies the intellectual and artistic spirit of Ireland, grappling with his own identity and place in the world. Their paths intersect on a series of seemingly ordinary events that become imbued with profound symbolism and metaphysical undertones. Through stream of consciousness, Joyce delves into the characters'' innermost thoughts, fears, and desires. The narrative unfolds in a kaleidoscope of perspectives, blurring the lines between reality and imagination. The city becomes a canvas upon which human experience unfolds, its streets filled with both the mundane and the extraordinary. Ulysses explores themes of identity, alienation, and the search for meaning in a rapidly changing world. Bloom''s journey becomes a metaphor for the human condition, as he navigates the complexities of urban life and his own inner struggles. Joyce challenges conventional storytelling, disorienting the reader and inviting them to actively engage with the text. The novel''s experimental prose, dense symbolism, and philosophical depth have solidified its reputation as a groundbreaking work of modernism. Critics have hailed it as a masterpiece that challenges established literary norms and pushes the boundaries of narrative and language. Through its complex characters and intricate tapestry of themes, Ulysses continues to captivate and provoke readers worldwide, cementing its place as a literary icon of the 20th century.');
INSERT INTO booksummaries (book_id, summary) VALUES (22, 'Marcus Aurelius'' "Meditations" is a collection of his personal thoughts and reflections on life, virtue, and the human condition. Written in the form of a journal, it offers a unique glimpse into the mind of a Roman emperor and Stoic philosopher. Aurelius believed that the key to a fulfilling life lay in accepting the limitations of existence and focusing on what was within one''s control. He emphasized the importance of self-discipline, reason, and compassion, as well as the need to live in harmony with nature. According to Aurelius, the pursuit of pleasure or external possessions was futile, as they could not bring true happiness or fulfillment. Instead, he advocated for a life of virtue, where one sought to improve one''s character and act in accordance with reason. He recognized that life was often filled with adversity, and that it was important to face challenges with courage and resilience. By accepting the inevitability of death and focusing on the present moment, one could find peace and contentment. Aurelius also stressed the importance of living in harmony with others. He believed that humans were social creatures and that their well-being was interconnected. He advocated for compassion, empathy, and the forgiveness of others, as well as the recognition that everyone had their own unique strengths and weaknesses. Overall, "Meditations" is a timeless work of philosophy that offers profound insights into the human condition and the path to a meaningful life. It is a reminder of the importance of virtue, self-discipline, and compassion, and the need to accept the limitations of existence while striving to improve ourselves and live in harmony with others.');
INSERT INTO booksummaries (book_id, summary) VALUES (20, 'In the lush Indian jungle, Mowgli, a young orphan boy, is adopted by a pack of wolves. Under the watchful eye of his guardians, Akela the wolf and Bagheera the panther, Mowgli learns the ways of the wild. However, the evil tiger, Shere Khan, seeks to hunt Mowgli down. Fearing for his son''s safety, Akela calls for a council of the pack to determine Mowgli''s fate. Shere Khan''s insidious plot is thwarted when Mowgli proves his loyalty to the pack by using fire to fend off the tiger. As Mowgli continues to grow, he befriends Baloo, a wise old bear, who teaches him the "Law of the Jungle." However, the boy''s insatiable curiosity leads him to question the laws and traditions of the animals, creating conflict with his mentors. During a severe drought, Mowgli encounters a group of monkeys who kidnap him and take him to their ancient city. There, he faces the machinations of King Louie, a giant ape who desires to learn the secrets of fire from Mowgli. With the help of Baloo and Bagheera, Mowgli outsmarts Louie and escapes the monkeys. Mowgli''s adventures culminate in a final confrontation with Shere Khan. Using his fire and the help of his animal friends, Mowgli triumphs over the tiger, proving his worthiness to stay in the jungle. However, as Mowgli matures, he begins to feel a disconnect between his human instincts and the ways of the animals. Torn between his love for the jungle and his longing for human society, Mowgli ultimately chooses to return to human civilization. Mowgli''s journey through the jungle is a tale of self-discovery, friendship, and the complexities of embracing one''s dual nature. It explores themes of respect for the natural world, the importance of tradition, and the challenges of balancing instinct and intellect.');

select
*
from
    pages where book_id = 12 order by page_number desc limit 100 ;

--commit transaction;
--insert into pages (
--    content,
--    page_number)
--values (
--    :content,
--    :page_number)
--;


SELECT
        b.title,
        left(p.content,500) as page,
        a.name,
        p.page_number,
        (p.embedding <=> embedding('textembedding-gecko@003','Give me the poems about love?')::vector) as distance
FROM
        pages p
JOIN books b on
        p.book_id=b.book_id
JOIN authors a on
       a.author_id=b.author_id
ORDER BY
        distance ASC
LIMIT 10;

SELECT
        b.title,
        left(p.content,500) as page,
        a.name,
        p.page_number,
        (p.embedding <=> embedding('textembedding-gecko@003', 'Find the paragraphs mentioning adventure, animals, coming-of-age in the book')::vector) as distance
FROM
        pages p
JOIN books b on
        p.book_id=b.book_id
JOIN authors a on
       a.author_id=b.author_id
WHERE b.title = 'The_Jungle_Book' AND a.name = 'Rudyard_Kipling'
ORDER BY distance ASC LIMIT 10;