-- Migrate asset types: PDF -> PNG, AUDIO -> MIDI
UPDATE assets SET type = 'PNG' WHERE type = 'PDF';
UPDATE assets SET type = 'MIDI' WHERE type = 'AUDIO';
