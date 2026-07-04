#!/bin/bash
input=$(cat)
current_dir=$(echo "$input" | jq -r '.workspace.current_dir // empty')
model=$(echo "$input" | jq -r '.model.display_name // empty')
remaining=$(echo "$input" | jq -r '.context_window.remaining_percentage // empty')

parts=""

if [ -n "$current_dir" ]; then
  parts="$current_dir"
fi

if [ -n "$model" ]; then
  parts="$parts | $model"
fi

if [ -n "$remaining" ]; then
  parts="$parts | Context: ${remaining}%"
fi

echo "$parts"